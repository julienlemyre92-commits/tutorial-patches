package net.runelite.client.plugins.microbot.questcommon.acquisition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.VarClientInt;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

/**
 * One caller-ticked GE buy. The caller owns travel, funding, exclusive input,
 * and durable checkpoint storage. No scheduler, walker, or background thread.
 * The loaded Rs2GrandExchange purchase/price methods are stubs; never call them.
 */
public final class QuestGeBuyer {
    public enum Outcome { WORKING, NEED_COINS, NEEDS_OVERVIEW, COMPLETE, CANCELLED, HOLD }
    private enum Phase {
        QUOTE, OPEN, WAIT_OPEN, SLOT, WAIT_SEARCH, TYPE_SEARCH,
        WAIT_RESULT, WAIT_ITEM, QUANTITY, WAIT_Q_INPUT, WAIT_Q_VALUE,
        PRICE, WAIT_P_INPUT, WAIT_P_VALUE, CONFIRM, WAIT_OFFER,
        WAIT_FILL, OPEN_OWNED, WAIT_OWNED, COLLECT_ITEM,
        WAIT_ITEM_COLLECT, COLLECT_COINS, WAIT_COIN_COLLECT, WAIT_SLOT_CLEAR,
        RECOVER_OPEN, RECOVER_WAIT_OPEN, RECOVER_OWNED, RECOVER_WAIT_OWNED,
        ABORT, WAIT_ABORT, COMPLETE, CANCELLED, HOLD
    }
    public static final class Result {
        public final Outcome outcome;
        public final String phase, reason;
        public final int slot, quotedPrice, itemCount, coins;
        public final long actualSpent;
        private Result(Outcome outcome, String phase, String reason,
                       int slot, int quotedPrice, int itemCount, int coins,long actualSpent) {
            this.outcome = outcome;
            this.phase = phase;
            this.reason = reason;
            this.slot = slot;
            this.quotedPrice = quotedPrice;
            this.itemCount = itemCount;
            this.coins = coins;
            this.actualSpent=actualSpent;
        }
    }
    private static final class Offer {
        final GrandExchangeOfferState state;
        final int item, total, filled;
        final long price, spent;
        Offer(GrandExchangeOffer raw) {
            state = raw == null ? GrandExchangeOfferState.EMPTY : raw.getState();
            item = raw == null ? 0 : raw.getItemId();
            total = raw == null ? 0 : raw.getTotalQuantity();
            filled = raw == null ? 0 : raw.getQuantitySold();
            price = raw == null ? 0 : raw.getPrice();
            spent = raw == null ? 0 : raw.getSpent();
        }
    }
    private static final class Frame {
        boolean loggedIn, overview, offerScreen, searchPrompt, inputVisible;
        int inventoryItem, coins, quantityVarbit, priceVarbit;
        Offer[] offers;
        Widget[] slots;
        Widget offerRoot, collectRoot, searchRoot, searchPromptWidget,
            inputWidget, confirmWidget;
    }

    private final int itemId, quantity, cap;
    private final String itemName, userAgent;
    private Phase phase = Phase.QUOTE;
    private int price, slot = -1, initialItem = -1, coinsAfterPlace = -1;
    private long quoteEpoch, phaseAt, fillAt, finalSpent, expectedRefund;
    private boolean chooseClicked, itemCollectClicked, coinCollectClicked,
        cancelled, refundProved, ownedOfferSeen;
    private String reason = "";

    public static QuestGeBuyer troutFour(String userAgent, String checkpoint) {
        return new QuestGeBuyer(333, "Trout", 4, 1000, userAgent, checkpoint);
    }

    /** cap is the remaining cumulative budget supplied by the quest caller. */
    public QuestGeBuyer(int itemId, String itemName, int quantity, int cap,
                        String userAgent, String checkpoint) {
        if (itemId <= 0 || itemName == null || itemName.isBlank() || quantity < 1
            || quantity > 28 || cap < 1 || cap > 1000)
            throw new IllegalArgumentException("Explicit item/quantity/remaining cap required");
        if (userAgent == null || userAgent.isBlank() || userAgent.length() < 15)
            throw new IllegalArgumentException("Descriptive Wiki API User-Agent required");
        this.itemId = itemId;
        this.itemName = itemName.trim();
        this.quantity = quantity;
        this.cap = cap;
        this.userAgent = userAgent;
        if (checkpoint != null && !checkpoint.isBlank()) restore(checkpoint);
    }

    /**
     * persist must durably store the checkpoint BEFORE each dispatched input.
     * A failed persist prevents that input. Call only from the script executor.
     */
    public synchronized Result tick(Consumer<String> persist) {
        if (persist == null) throw new IllegalArgumentException("checkpoint persistor required");
        Frame f;
        try { f = Microbot.getClientThread().invoke((Supplier<Frame>) this::frame); }
        catch (Exception ex) { return hold(persist, "client snapshot failed: " + ex); }
        if (f == null || !f.loggedIn || f.offers == null)
            return result(Outcome.WORKING, f, "waiting for logged-in client/offer snapshot");
        if (phase == Phase.HOLD) return result(Outcome.HOLD, f, reason);
        if (phase == Phase.COMPLETE) return result(Outcome.COMPLETE, f, reason);
        if (phase == Phase.CANCELLED) return result(Outcome.CANCELLED, f, reason);

        if (phase == Phase.QUOTE) {
            try {
                int candidate = fetchPrice();
                if ((long) candidate * quantity > cap)
                    return hold(persist, "fresh quote exceeds remaining cap");
                price = candidate;
                quoteEpoch = System.currentTimeMillis();
                move(Phase.OPEN, persist);
            } catch (Exception ex) { return hold(persist, "Wiki quote unavailable: " + ex); }
            return result(Outcome.WORKING, f, "fresh quote accepted");
        }
        if (initialItem < 0) { initialItem = f.inventoryItem; persist.accept(checkpoint()); }
        if (slot >= f.offers.length)
            return hold(persist, "owned slot absent from current client offer array");
        if (slot >= 0 && isForeign(f.offers[slot]))
            return hold(persist, "owned slot changed to a different item/quantity/price");
        if (slot >= 0 && owned(f.offers[slot])) {
            boolean changed = !ownedOfferSeen || finalSpent != f.offers[slot].spent
                || coinsAfterPlace < 0 || f.coins < coinsAfterPlace;
            ownedOfferSeen = true;
            finalSpent = f.offers[slot].spent;
            if (coinsAfterPlace < 0 || f.coins < coinsAfterPlace)
                coinsAfterPlace = f.coins;
            if (finalSpent < 0 || finalSpent > cap)
                return hold(persist, "owned offer spent amount exceeded cap");
            if (changed) persist.accept(checkpoint());
        }
        long now = System.currentTimeMillis();
        switch (phase) {
            case RECOVER_OPEN:
                if(!recoverableOwned(f))return hold(persist,"owned filled offer changed during collection recovery");
                if(offerShowsItem(f)){move(Phase.COLLECT_ITEM,persist);break;}
                if(f.overview){move(Phase.RECOVER_OWNED,persist);break;}
                if(f.offerScreen || f.searchPrompt)
                    return hold(persist,"foreign GE form during collection recovery");
                Rs2NpcModel recoveryClerk=Rs2Npc.getNpc("Grand Exchange Clerk");
                if(recoveryClerk==null)
                    return result(Outcome.WORKING,f,"reach GE clerk to reopen owned offer");
                move(Phase.RECOVER_WAIT_OPEN,persist);
                if(!Rs2Npc.interact(recoveryClerk,"Exchange"))
                    return hold(persist,"recovery Exchange interaction rejected; no repeat");
                break;
            case RECOVER_WAIT_OPEN:
                if(!recoverableOwned(f))return hold(persist,"owned offer changed after recovery open");
                if(f.overview)move(Phase.RECOVER_OWNED,persist);
                else if(expired(now,8000))return hold(persist,"recovery GE overview unproved; no repeat");
                break;
            case RECOVER_OWNED:
                if(!recoverableOwned(f))return hold(persist,"owned offer changed before recovery view");
                if(offerShowsItem(f)){move(Phase.COLLECT_ITEM,persist);break;}
                if(!f.overview || !visible(f.slots[slot]))
                    return hold(persist,"owned recovery slot unavailable");
                move(Phase.RECOVER_WAIT_OWNED,persist);
                if(!Rs2Widget.clickWidget(f.slots[slot]))
                    return hold(persist,"owned recovery view click rejected; no repeat");
                break;
            case RECOVER_WAIT_OWNED:
                if(!recoverableOwned(f))return hold(persist,"owned offer changed after recovery view");
                if(offerShowsItem(f))move(Phase.COLLECT_ITEM,persist);
                else if(expired(now,6000))return hold(persist,"owned recovery detail unproved; no repeat");
                break;
            case OPEN:
                if (f.overview) { move(Phase.SLOT, persist); break; }
                if (f.offerScreen || f.searchPrompt)
                    return result(Outcome.NEEDS_OVERVIEW, f,
                        "caller must close the pre-open GE form before a fresh buy");
                Rs2NpcModel clerk = Rs2Npc.getNpc("Grand Exchange Clerk");
                if (clerk == null) return result(Outcome.WORKING, f, "caller must reach GE clerk");
                move(Phase.WAIT_OPEN, persist);
                if (!Rs2Npc.interact(clerk, "Exchange"))
                    return hold(persist, "clerk Exchange interaction rejected");
                break;
            case WAIT_OPEN:
                if (f.overview) move(Phase.SLOT, persist);
                else if (expired(now, 8000)) return hold(persist, "GE overview did not open");
                break;
            case SLOT:
                if (!f.overview && slot < 0) { move(Phase.OPEN, persist); break; }
                if (!f.overview) return hold(persist, "GE overview closed after slot selection");
                if (f.coins < (long) price * quantity)
                    return result(Outcome.NEED_COINS, f,
                        "withdraw at least " + (price * quantity) + " coins first");
                for (Offer o : f.offers)
                    if (o.item == itemId && o.state != GrandExchangeOfferState.EMPTY)
                        return hold(persist, "matching existing offer: ownership unknown");
                slot = firstAvailable(f);
                if (slot < 0) return hold(persist, "no verified interactive EMPTY slot");
                persist.accept(checkpoint());
                Widget buy = child(f.slots[slot],3);
                move(Phase.WAIT_SEARCH, persist);
                if (!Rs2Widget.clickWidget(buy)) return hold(persist, "buy button click rejected");
                break;
            case WAIT_SEARCH:
                if (f.searchPrompt) { move(Phase.TYPE_SEARCH, persist); break; }
                if (f.offerScreen && !chooseClicked) {
                    Widget choose = child(f.offerRoot, 0);
                    if (visible(choose)) {
                        chooseClicked = true;
                        persist.accept(checkpoint());
                        if (!Rs2Widget.clickWidget(choose))
                            return hold(persist, "choose-item click rejected");
                        phaseAt = now;
                        persist.accept(checkpoint());
                    }
                }
                if (expired(now, 7000)) return hold(persist, "GE item search did not open");
                break;
            case TYPE_SEARCH:
                if (!f.searchPrompt) return hold(persist, "item search vanished before typing");
                move(Phase.WAIT_RESULT, persist);
                Rs2Keyboard.typeString(itemName);
                break;
            case WAIT_RESULT:
                Widget match = exactResult(f.searchRoot, itemName);
                if (match != null) {
                    move(Phase.WAIT_ITEM, persist);
                    if (!Rs2Widget.clickWidget(match))
                        return hold(persist, "exact item result click rejected");
                } else if (expired(now, 6000))
                    return hold(persist, "exact item search result absent");
                break;
            case WAIT_ITEM:
                if (f.offerScreen && !f.searchPrompt && offerShowsItem(f))
                    move(Phase.QUANTITY, persist);
                else if (expired(now, 6000))
                    return hold(persist, "selected item not proved on offer screen");
                break;
            case QUANTITY:
                if (!offerShowsItem(f)) return hold(persist, "item proof lost before quantity");
                if (f.quantityVarbit == quantity) { move(Phase.PRICE, persist); break; }
                Widget qx = child(f.offerRoot, 7);
                if (!visible(qx)) return hold(persist, "quantity X widget unavailable");
                move(Phase.WAIT_Q_INPUT, persist);
                if (!Rs2Widget.clickWidget(qx)) return hold(persist, "quantity X click rejected");
                break;
            case WAIT_Q_INPUT:
                if (f.inputVisible) {
                    move(Phase.WAIT_Q_VALUE, persist);
                    Rs2Keyboard.typeString(Integer.toString(quantity));
                    Rs2Keyboard.enter();
                } else if (expired(now, 3500))
                    return hold(persist, "quantity chatbox input not observed");
                break;
            case WAIT_Q_VALUE:
                if (f.quantityVarbit == quantity) move(Phase.PRICE, persist);
                else if (expired(now, 3500)) return hold(persist, "quantity value not verified");
                break;
            case PRICE:
                if (!offerShowsItem(f) || f.quantityVarbit != quantity)
                    return hold(persist, "item/quantity proof lost before price");
                if (f.priceVarbit == price) { move(Phase.CONFIRM, persist); break; }
                Widget px = child(f.offerRoot, 12);
                if (!visible(px)) return hold(persist, "price X widget unavailable");
                move(Phase.WAIT_P_INPUT, persist);
                if (!Rs2Widget.clickWidget(px)) return hold(persist, "price X click rejected");
                break;
            case WAIT_P_INPUT:
                if (f.inputVisible) {
                    move(Phase.WAIT_P_VALUE, persist);
                    Rs2Keyboard.typeString(Integer.toString(price));
                    Rs2Keyboard.enter();
                } else if (expired(now, 3500))
                    return hold(persist, "price chatbox input not observed");
                break;
            case WAIT_P_VALUE:
                if (f.priceVarbit == price) move(Phase.CONFIRM, persist);
                else if (expired(now, 3500)) return hold(persist, "price value not verified");
                break;
            case CONFIRM:
                if (!offerShowsItem(f) || f.quantityVarbit != quantity
                    || f.priceVarbit != price || f.coins < (long) price * quantity
                    || f.offers[slot].state != GrandExchangeOfferState.EMPTY
                    || now - quoteEpoch > 1800000)
                    return hold(persist, "offer/cap/coin proof changed before confirm");
                Widget confirm = f.confirmWidget;
                if (!hasAction(confirm, "Confirm")) confirm = null;
                if (!visible(confirm)) return hold(persist, "confirm widget absent");
                move(Phase.WAIT_OFFER, persist);
                if (!Rs2Widget.clickWidget(confirm)) return hold(persist, "confirm click rejected");
                break;
            case WAIT_OFFER:
                if (owned(f.offers[slot])) {
                    ownedOfferSeen = true;
                    coinsAfterPlace = f.coins;
                    fillAt = now;
                    move(Phase.WAIT_FILL, persist);
                } else if (expired(now, 8000))
                    return hold(persist, "owned offer not observed after confirm; no repeat");
                break;
            case WAIT_FILL:
                if (f.offers[slot].state == GrandExchangeOfferState.BOUGHT
                    || f.offers[slot].filled >= quantity) {
                    cancelled = false;
                    move(Phase.OPEN_OWNED, persist);
                } else if (f.offers[slot].state == GrandExchangeOfferState.CANCELLED_BUY) {
                    cancelled = true;
                    move(Phase.OPEN_OWNED, persist);
                } else if (now - fillAt > 90000) {
                    move(Phase.ABORT, persist);
                }
                break;
            case ABORT:
                if (!owned(f.offers[slot])) return hold(persist, "offer ownership lost before abort");
                if (f.offers[slot].state == GrandExchangeOfferState.BOUGHT) {
                    cancelled = false;
                    move(Phase.OPEN_OWNED, persist); break;
                }
                Widget abort = findPrimaryAction(f.offerRoot, "Abort offer");
                if (!visible(abort)) {
                    if (!f.offerScreen) { move(Phase.OPEN_OWNED, persist); break; }
                    return hold(persist, "owned abort control unavailable; offer left untouched");
                }
                move(Phase.WAIT_ABORT, persist);
                if (!Rs2Widget.clickWidget(abort)) return hold(persist, "abort click rejected");
                break;
            case WAIT_ABORT:
                if (f.offers[slot].state == GrandExchangeOfferState.CANCELLED_BUY) {
                    cancelled = true;
                    move(Phase.OPEN_OWNED, persist);
                }
                else if (expired(now, 6000))
                    return hold(persist, "abort not proved; no repeat");
                break;
            case OPEN_OWNED:
                if (!owned(f.offers[slot])) return hold(persist, "owned offer changed before view");
                if (f.offerScreen && offerShowsItem(f)) {
                    move(f.offers[slot].state == GrandExchangeOfferState.BUYING
                        ? Phase.ABORT : Phase.COLLECT_ITEM, persist);
                    break;
                }
                if (!f.overview || !visible(f.slots[slot]))
                    return hold(persist, "owned offer slot not visible");
                move(Phase.WAIT_OWNED, persist);
                if (!Rs2Widget.clickWidget(f.slots[slot]))
                    return hold(persist, "owned offer view click rejected");
                break;
            case WAIT_OWNED:
                if (f.offerScreen && offerShowsItem(f))
                    move(f.offers[slot].state == GrandExchangeOfferState.BUYING
                        ? Phase.ABORT : Phase.COLLECT_ITEM, persist);
                else if (expired(now, 6000))
                    return hold(persist, "owned offer detail not proved");
                break;
            case COLLECT_ITEM:
                if (!offerShowsItem(f) || !owned(f.offers[slot]))
                    return hold(persist, "ownership/detail proof lost before item collect");
                if (f.offers[slot].filled <= 0 || itemCollectClicked) {
                    move(Phase.COLLECT_COINS, persist); break;
                }
                Widget itemCollect = findCollect(f.collectRoot, itemId);
                if (!visible(itemCollect)) return hold(persist, "owned item collect control absent");
                itemCollectClicked = true;
                move(Phase.WAIT_ITEM_COLLECT, persist);
                if (!clickVerifiedAction(f.collectRoot,itemCollect,collectAction(itemCollect,itemId)))
                    return hold(persist, "item collect click rejected");
                break;
            case WAIT_ITEM_COLLECT:
                if (f.inventoryItem > initialItem) move(Phase.COLLECT_COINS, persist);
                else if (expired(now, 6000))
                    return hold(persist, "item inventory delta not proved; no repeat");
                break;
            case COLLECT_COINS:
                if (!ownedOfferSeen)
                    return hold(persist, "owned offer was never observed");
                if (f.offers[slot].state != GrandExchangeOfferState.EMPTY
                    && f.offers[slot].state != GrandExchangeOfferState.BOUGHT
                    && f.offers[slot].state != GrandExchangeOfferState.CANCELLED_BUY)
                    return hold(persist, "offer not complete/cancelled before coin collection");
                if (!cancelled && f.inventoryItem < initialItem + quantity)
                    return hold(persist, "buy complete but item inventory short");
                expectedRefund = (long) price * quantity - finalSpent;
                if (expectedRefund < 0 || expectedRefund > cap)
                    return hold(persist, "refund exceeds bounded reservation");
                if (expectedRefund == 0) {
                    refundProved = true;
                    move(Phase.WAIT_SLOT_CLEAR, persist);
                    break;
                }
                if (coinsAfterPlace >= 0
                    && f.coins >= (long) coinsAfterPlace + expectedRefund) {
                    refundProved = true;
                    move(Phase.WAIT_SLOT_CLEAR, persist);
                    break;
                }
                if (f.offers[slot].state == GrandExchangeOfferState.EMPTY)
                    return hold(persist, "slot cleared but required refund not in inventory");
                if (coinCollectClicked) { move(Phase.WAIT_COIN_COLLECT, persist); break; }
                Widget refund = findCollect(f.collectRoot, 995);
                if (!visible(refund)) return hold(persist, "owned coin refund control absent");
                coinCollectClicked = true;
                move(Phase.WAIT_COIN_COLLECT, persist);
                if (!clickVerifiedAction(f.collectRoot,refund,collectAction(refund,995)))
                    return hold(persist, "coin refund click rejected");
                break;
            case WAIT_COIN_COLLECT:
                if (coinsAfterPlace >= 0
                    && f.coins >= (long) coinsAfterPlace + expectedRefund) {
                    refundProved = true;
                    move(Phase.WAIT_SLOT_CLEAR, persist);
                } else if (expired(now, 6000))
                    return hold(persist, "coin refund not proved; no repeat");
                break;
            case WAIT_SLOT_CLEAR:
                if (!ownedOfferSeen || !refundProved)
                    return hold(persist, "offer/refund proof missing at final boundary");
                if (f.offers[slot].state != GrandExchangeOfferState.EMPTY) {
                    if (expired(now, 6000))
                        return hold(persist, "owned offer slot did not clear");
                    break;
                }
                if (cancelled) {
                    move(Phase.CANCELLED, persist);
                    reason = "owned offer cancelled, refund and slot clearance verified; items="
                        + (f.inventoryItem - initialItem);
                } else {
                    if (f.inventoryItem < initialItem + quantity)
                        return hold(persist, "slot clear but requested inventory delta absent");
                    move(Phase.COMPLETE, persist);
                    reason = "owned buy, spend, inventory, refund and slot clearance verified";
                }
                persist.accept(checkpoint());
                break;
            default: return hold(persist, "unknown buyer phase");
        }
        return result(phase == Phase.COMPLETE ? Outcome.COMPLETE : Outcome.WORKING, f, reason);
    }

    private Frame frame() {
        Frame f = new Frame();
        Client c = Microbot.getClient();
        if (c == null || c.getGameState() != GameState.LOGGED_IN
            || c.getLocalPlayer() == null) return f;
        f.loggedIn = true;
        GrandExchangeOffer[] raw = c.getGrandExchangeOffers();
        if (raw == null || raw.length < 3) return f;
        f.offers = new Offer[raw.length];
        f.slots = new Widget[raw.length];
        for (int i = 0; i < raw.length; i++) {
            f.offers[i] = new Offer(raw[i]);
            f.slots[i] = i < 8 ? c.getWidget(465, 7 + i) : null;
        }
        f.inventoryItem = inventoryQuantity(c,itemId);
        f.coins = inventoryQuantity(c,995);
        f.offerRoot = visible(c.getWidget(465,26))?c.getWidget(465,26):c.getWidget(465,15);
        f.offerScreen = visible(f.offerRoot)
            && containsText(f.offerRoot, "Buy offer");
        f.overview = visible(c.getWidget(465, 1))
            && visible(c.getWidget(465, 5)) && !f.offerScreen;
        f.collectRoot = c.getWidget(465, 24);
        f.confirmWidget = c.getWidget(465, 30);
        f.searchRoot = c.getWidget(162, 53);
        f.searchPromptWidget = c.getWidget(162, 53);
        f.searchPrompt = visible(f.searchPromptWidget)
            && containsText(f.searchPromptWidget,
                "Start typing the name of an item");
        f.inputWidget = c.getWidget(162, 44);
        f.inputVisible = visible(f.inputWidget)
            && c.getVarcIntValue(VarClientInt.INPUT_TYPE) != 0;
        f.quantityVarbit = numericWidget(child(f.offerRoot,34));
        f.priceVarbit = numericWidget(child(f.offerRoot,41));
        return f;
    }

    private static int inventoryQuantity(Client c,int id) {
        var container=c.getItemContainer(net.runelite.api.InventoryID.INVENTORY);
        if(container==null) return 0;
        int total=0; for(var item:container.getItems()) if(item!=null&&item.getId()==id) total+=item.getQuantity();
        return total;
    }
    private static int numericWidget(Widget widget) {
        if(!visible(widget)) return -1;
        String value=clean(widget.getText()).replace(",", "").replace(" coins", "").trim();
        if(!value.matches("[0-9]+")) return -1;
        try { return Integer.parseInt(value); } catch(NumberFormatException e) { return -1; }
    }
    private int fetchPrice() throws Exception {
        return previewPrice(itemId,userAgent);
    }
    /** Same quote formula used by the live-proved buyer. Never a trade fill guarantee. */
    public static int previewPrice(int itemId,String userAgent) throws Exception {
        java.net.HttpURLConnection connection=(java.net.HttpURLConnection)
            URI.create("https://prices.runescape.wiki/api/v1/osrs/latest?id="+itemId).toURL().openConnection();
        connection.setConnectTimeout(4000); connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent",userAgent);
        connection.setRequestProperty("Accept","application/json");
        String responseBody;
        try {
            int status=connection.getResponseCode();
            if(status!=200) throw new IllegalStateException("HTTP "+status);
            try(var stream=connection.getInputStream()) {
                responseBody=new String(stream.readNBytes(65536),java.nio.charset.StandardCharsets.UTF_8);
            }
        } finally { connection.disconnect(); }
        JsonObject root = new JsonParser().parse(responseBody).getAsJsonObject();
        JsonObject item = root.getAsJsonObject("data").getAsJsonObject(Integer.toString(itemId));
        if (item == null || !item.has("high") || item.get("high").isJsonNull()
            || !item.has("highTime") || item.get("highTime").isJsonNull())
            throw new IllegalStateException("item quote or high timestamp absent");
        int high = item.get("high").getAsInt();
        long highTime = item.get("highTime").getAsLong();
        long now = System.currentTimeMillis() / 1000;
        if (high <= 0 || highTime < now - 1800 || highTime > now + 60)
            throw new IllegalStateException("invalid/stale high quote");
        long candidate = Math.max((long) high + 2, ((long) high * 5 + 3) / 4);
        if (candidate > Integer.MAX_VALUE) throw new IllegalStateException("quote overflow");
        return (int) candidate;
    }

    private int firstAvailable(Frame f) {
        for (int i = 0; i < Math.min(8, f.offers.length); i++) {
            Widget w = f.slots[i], buy = child(w, 3);
            if (f.offers[i].state == GrandExchangeOfferState.EMPTY && visible(buy)
                && hasAction(buy, "Create Buy offer")) return i;
        }
        return -1;
    }
    private boolean owned(Offer o) {
        return o != null && o.item == itemId && o.total == quantity
            && o.price == price && o.state != GrandExchangeOfferState.EMPTY;
    }
    private boolean isForeign(Offer o) {
        return o != null && o.state != GrandExchangeOfferState.EMPTY && !owned(o);
    }
    private boolean offerShowsItem(Frame f) {
        return f.offerScreen && visible(f.offerRoot)
            && (containsText(f.offerRoot, itemName)
                || containsItem(f.offerRoot, itemId));
    }
    private boolean recoverableOwned(Frame f){
        return f!=null && f.offers!=null && slot>=0 && slot<f.offers.length
            && owned(f.offers[slot]) && f.offers[slot].state==GrandExchangeOfferState.BOUGHT
            && f.offers[slot].filled==quantity && f.offers[slot].spent==finalSpent
            && f.inventoryItem==initialItem && f.coins==coinsAfterPlace
            && !itemCollectClicked && !coinCollectClicked && !cancelled && ownedOfferSeen;
    }
    private boolean expired(long now, long limit) {
        return phaseAt > 0 && now - phaseAt > limit;
    }
    private void move(Phase next, Consumer<String> persist) {
        phase = next;
        phaseAt = System.currentTimeMillis();
        persist.accept(checkpoint());
    }
    private Result hold(Consumer<String> persist, String why) {
        reason = why;
        move(Phase.HOLD, persist);
        return result(Outcome.HOLD, null, why);
    }
    private Result result(Outcome outcome, Frame f, String why) {
        return new Result(outcome, phase.name(), why, slot, price,
            f == null ? -1 : f.inventoryItem, f == null ? -1 : f.coins,finalSpent);
    }

    /** Resume only an owned, filled purchase whose item-collection action was never issued. */
    public synchronized boolean resumeOwnedCollection(Consumer<String> persist) {
        if(phase!=Phase.HOLD || itemCollectClicked || coinCollectClicked || cancelled || !ownedOfferSeen)return false;
        Frame f=Microbot.getClientThread().invoke((Supplier<Frame>)this::frame);
        if(f==null || !f.loggedIn || !recoverableOwned(f))return false;
        if(offerShowsItem(f)){
            if(findCollect(f.collectRoot,itemId)==null)return false;
            move(Phase.COLLECT_ITEM,persist);
        }else move(Phase.RECOVER_OPEN,persist);
        return true;
    }

    /** Stable primitive checkpoint; never serialize Widget or client objects. */
    public synchronized String checkpoint() {
        return "GE2|" + itemId + '|' + quantity + '|' + cap + '|' + phase.name()
            + '|' + price + '|' + slot + '|' + initialItem + '|' + coinsAfterPlace
            + '|' + quoteEpoch + '|' + phaseAt + '|' + fillAt + '|'
            + (chooseClicked ? 1 : 0) + '|' + (itemCollectClicked ? 1 : 0)
            + '|' + (coinCollectClicked ? 1 : 0) + '|' + (cancelled ? 1 : 0)
            + '|' + (refundProved ? 1 : 0) + '|' + (ownedOfferSeen ? 1 : 0)
            + '|' + finalSpent + '|' + expectedRefund;
    }
    private void restore(String value) {
        String[] a = value.split("\\|", -1);
        if (a.length != 20 || !"GE2".equals(a[0])
            || Integer.parseInt(a[1]) != itemId
            || Integer.parseInt(a[2]) != quantity
            || Integer.parseInt(a[3]) != cap)
            throw new IllegalArgumentException("foreign/invalid GE checkpoint");
        phase = Phase.valueOf(a[4]);
        price = Integer.parseInt(a[5]);
        slot = Integer.parseInt(a[6]);
        initialItem = Integer.parseInt(a[7]);
        coinsAfterPlace = Integer.parseInt(a[8]);
        quoteEpoch = Long.parseLong(a[9]);
        phaseAt = Long.parseLong(a[10]);
        fillAt = Long.parseLong(a[11]);
        chooseClicked = "1".equals(a[12]);
        itemCollectClicked = "1".equals(a[13]);
        coinCollectClicked = "1".equals(a[14]);
        cancelled = "1".equals(a[15]);
        refundProved = "1".equals(a[16]);
        ownedOfferSeen = "1".equals(a[17]);
        finalSpent = Long.parseLong(a[18]);
        expectedRefund = Long.parseLong(a[19]);
        if (slot < -1 || slot > 7 || price < 0 || initialItem < -1)
            throw new IllegalArgumentException("invalid GE checkpoint values");
        reason = "restored; verify pending action before any new input";
    }

    private static Widget child(Widget parent, int index) {
        if(!Microbot.getClient().isClientThread()) return Microbot.getClientThread().invoke((Supplier<Widget>)()->child(parent,index));
        return parent == null ? null : parent.getChild(index);
    }
    private static boolean visible(Widget w) {
        if(!Microbot.getClient().isClientThread()) return Microbot.getClientThread().invoke((Supplier<Boolean>)()->visible(w));
        return w != null && !w.isHidden() && w.getBounds() != null
            && w.getBounds().width > 1 && w.getBounds().height > 1;
    }
    private static String clean(String s) {
        return s == null ? "" : s.replaceAll("<[^>]*>", "").trim()
            .toLowerCase(Locale.ROOT);
    }
    private static boolean hasAction(Widget w, String action) {
        if(!Microbot.getClient().isClientThread()) return Microbot.getClientThread().invoke((Supplier<Boolean>)()->hasAction(w,action));
        if (w == null || w.getActions() == null) return false;
        for (String a : w.getActions())
            if (clean(a).equals(clean(action))) return true;
        return false;
    }
    private static boolean primaryAction(Widget w, String action) {
        if(!Microbot.getClient().isClientThread()) return Microbot.getClientThread().invoke((Supplier<Boolean>)()->primaryAction(w,action));
        String[] actions = w == null ? null : w.getActions();
        return actions != null && actions.length > 0
            && clean(actions[0]).equals(clean(action));
    }
    private static Widget exactResult(Widget root, String name) {
        if(!Microbot.getClient().isClientThread()) return Microbot.getClientThread().invoke((Supplier<Widget>)()->exactResult(root,name));
        if (!visible(root) || root.getChildren() == null) return null;
        Widget[] children = root.getChildren();
        for (int i = 1; i < children.length; i++)
            if (visible(children[i]) && clean(children[i].getText()).equals(clean(name))
                && visible(children[i - 1])) return children[i - 1];
        return null;
    }
    private static boolean containsText(Widget root, String text) {
        return find(root, w -> clean(w.getText()).contains(clean(text))) != null;
    }
    private static boolean containsItem(Widget root, int item) {
        return find(root, w -> w.getItemId() == item) != null;
    }
    private static Widget findActionOrText(Widget root, String text) {
        return find(root, w -> hasAction(w, text)
            || clean(w.getText()).equals(clean(text)));
    }
    private static Widget findPrimaryAction(Widget root, String action) {
        return find(root, w -> primaryAction(w, action));
    }
    static record CollectControl(int itemId,Set<String> actions) {
        boolean has(String action){return actions.contains(clean(action));}
    }
    private static String collectAction(Widget w,int item){
        if(item!=995){
            for(String action:List.of("Collect-item","Collect-items"))
                if(hasAction(w,action))return action;
        }
        return "Collect";
    }
    private Widget findCollect(Widget root,int item){
        if(!Microbot.getClient().isClientThread())
            return Microbot.getClientThread().invoke((Supplier<Widget>)()->findCollect(root,item));
        if(root==null || root.isHidden() || root.getChildren()==null)return null;
        // Installed GrandExchangeWidget.getCollectButtons reads direct children of
        // (465,24) whose actions contain "collect". Item icons need not be the
        // action widget, so binding itemId and action to one widget misses them.
        List<Widget> buttons=new ArrayList<>();
        List<CollectControl> controls=new ArrayList<>();
        for(Widget child:root.getChildren()){
            if(!visible(child) || child.getActions()==null)continue;
            Set<String> actions=new java.util.HashSet<>();
            for(String action:child.getActions())if(action!=null)actions.add(clean(action));
            if(actions.stream().noneMatch(a->a.startsWith("collect")))continue;
            buttons.add(child);controls.add(new CollectControl(child.getItemId(),actions));
        }
        int index=selectCollectIndex(controls,item,itemId);
        return index<0?null:buttons.get(index);
    }
    /** Returns no control on ambiguity; never sends a broad collect-all action. */
    static int selectCollectIndex(List<CollectControl> controls,int item,int ownedItem){
        if(item!=995 && item!=ownedItem)return -1;
        if(controls.isEmpty() || controls.size()>2)return -1;
        int exact=-1;
        for(int i=0;i<controls.size();i++)if(controls.get(i).itemId()==item){
            if(exact>=0)return -1;
            exact=i;
        }
        if(exact>=0){
            CollectControl c=controls.get(exact);
            return item==995?c.has("Collect")?exact:-1
                :c.has("Collect-item") || c.has("Collect-items") || c.has("Collect")?exact:-1;
        }
        // A distinct item action is conclusive even when the action widget has
        // no itemId. Coins have no such action in the installed GE adapter.
        if(item!=995){
            int itemAction=-1;
            for(int i=0;i<controls.size();i++){
                CollectControl c=controls.get(i);
                if(c.has("Collect-items") || c.has("Collect-item")){
                    if(itemAction>=0)return -1;
                    itemAction=i;
                }
            }
            if(itemAction>=0 && controls.get(itemAction).itemId()!=995)return itemAction;
        }
        // Installed Rs2GrandExchange maps the first collect child to param0=2
        // and the second to param0=3. Use this only for the exact owned detail
        // already proved by the caller, with at most two collect controls.
        if(controls.size()==2){
            int i=item==995?1:0;
            CollectControl chosen=controls.get(i),other=controls.get(1-i);
            if(chosen.itemId()>0 && chosen.itemId()!=item)return -1;
            if(other.itemId()!= (item==995?ownedItem:995))return -1;
            return chosen.has("Collect")?i:-1;
        }
        return -1; // A lone generic control could be an uncollected refund.
    }
    private static boolean clickVerifiedAction(Widget root,Widget w,String action) {
        int[] args=Microbot.getClientThread().invoke((Supplier<int[]>)()->{
            if(!visible(w)||root==null||root.isHidden()||root.getChildren()==null
                ||w.getActions()==null)return null;
            int ordinal=0,param0=-1;
            for(Widget child:root.getChildren()){
                if(child==null || child.getActions()==null)continue;
                boolean collect=false;
                for(String a:child.getActions())
                    if(a!=null && clean(a).startsWith("collect")){collect=true;break;}
                if(!collect)continue;
                if(child==w){param0=2+ordinal;break;}
                ordinal++;
            }
            if(param0<2 || param0>3)return null;
            String[] actions=w.getActions();
            for(int i=0;i<actions.length;i++) if(clean(actions[i]).equals(clean(action)))
                return new int[]{param0,i+1};
            return null;
        });
        if(args==null) return false;
        Rs2Widget.clickWidgetFast(w,args[0],args[1]); return true;
    }
    private interface Test { boolean yes(Widget w); }
    private static Widget find(Widget root, Test test) {
        if(!Microbot.getClient().isClientThread()) return Microbot.getClientThread().invoke((Supplier<Widget>)()->find(root,test));
        Set<Widget> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        return find(root, test, seen, 0);
    }
    private static Widget find(Widget w, Test test, Set<Widget> seen, int depth) {
        if (!visible(w) || depth > 8 || !seen.add(w)) return null;
        if (test.yes(w)) return w;
        for (Widget[] a : new Widget[][] {w.getChildren(), w.getDynamicChildren(), w.getStaticChildren()})
            if (a != null) for (Widget child : a) {
                Widget found = find(child, test, seen, depth + 1);
                if (found != null) return found;
            }
        return null;
    }
}

