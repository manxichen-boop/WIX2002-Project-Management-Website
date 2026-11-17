# Changelog

## 1.1.1 — 13 November 2026

- New artwork for Stationery, Sports Equipment and Household (supplied), and a Reusable Materials icon redrawn in the same style.
- "More" now **rotates the category chips around the green circle** (counter-clockwise, 1.5 s, eased): the main categories travel over the top and leave at the bottom left while the other categories come out from behind the More chip. "Back" turns the circle the other way. Phones keep the grid; reduced-motion users get an instant switch.
- The Back chip uses the leaf-arrow artwork instead of the dark button.

## 1.1.0 — 13 November 2026

- **Category artwork.** Stationery, Sports Equipment and Household now use the supplied illustrations; Reusable Materials has a matching illustration. Admins can choose these icons in Admin › Categories (migration V16 assigns them).
- **"More" on the homepage** flips the five main category chips over to show the other categories (with a Back chip). On phones the chips form a tidy grid under the picture. Without JavaScript, More still opens Explore.
- **Pickup maps (OpenStreetMap, no API key).**
  - Post / Edit item: optional map pin. Search a place, tap the map, or use the device location; the pin can be dragged. An empty "Pickup location" text is filled from the map.
  - Item page: pickup map, straight-line distance and an approximate walking time from the visitor, plus a Walking directions link.
  - New **Pickup map** page (`/pickup-map`, in the Explore menu and the student sidebar) following the "Find Pickup Points" design: list + map, closest points first, routes to the nearest three, category filter and search.
  - The visitor's location stays in their browser (session storage) and is never sent to the server.
  - Default map centre: Universiti Malaya; change with `ECOSWAP_MAP_CENTER="lat,lng"` and `ECOSWAP_MAP_ZOOM`.
- **Chat.** Owner and requester can chat once a request is accepted (Messages page, header and bottom-nav badges, notifications). Messages are stored (table `chat_messages`, migration V17), new ones appear within ~3 s, read receipts show "Seen", and the handover can be confirmed from the chat. Chats of withdrawn exchanges stay readable but closed.
- **Completed exchanges / recorded weight.** The figures were correct but an exchange only completes after *both* students confirm the handover, and nothing made that clear. Request cards and the chat now say who still has to confirm, the toast says when the exchange is completed, and badges refresh immediately. Weight counts only items whose owner entered an approximate weight.
- **Responsive fixes.** No horizontal scrolling on any page at 375 px (header actions on small phones, dashboard and community grids).
- Windows: `mvnw` keeps its executable bit in git.
