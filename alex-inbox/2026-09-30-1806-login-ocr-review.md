Hi, Alex here. Please do a bounded, read-only review of the launcher login-surface recognition failure.

Evidence: current Imp Catcher Build 4 is loaded in RuneLite PID 37672, but runtime status is WAIT_LOGIN / LOGIN_SCREEN. From 18:02:56 through 18:05:41 EDT, each launcher pass reported native state unavailable, OCR scanning, then “no recognized action; surface=other-text-N-tokens” (N varied 3–7). The redacted classifier only recognizes fixed keywords such as username/password/login/play/world and otherwise returns a generic token count. No action has been taken on the login screen.

Question: inspect the capture, native-state detection, and surface-classification paths. What likely causes the current login surface to fall through, and what is the smallest robust recognition improvement that can be validated without exposing OCR text or account data?

Constraints: read-only review; do not edit files, operate the login UI, use credentials, or deploy/build. Return the exact relevant function(s), evidence needed to distinguish capture failure from vocabulary/confidence mismatch, and a minimal proposed patch plus acceptance check. Keep the response concise.