# EventNest - Current Project Status

*Last Updated: October 2026*

---

## 1. Executive Summary

**EventNest** is a native Android event discovery, ticket booking, and organizer/admin management platform built with modern Android development standards and Firebase cloud infrastructure.

The project is built entirely in **Kotlin (100% Kotlin)** with Kotlin DSL build configuration (`build.gradle.kts`) and Google Material Design 3.

### Key Highlights
- **100% Kotlin codebase**: Zero legacy Java code.
- **Full Firebase integration**: Authentication, Cloud Firestore (real-time listeners & atomic transactions), Firebase Storage, and Security Rules.
- **End-to-End User Flow**: Splash screen $\rightarrow$ Authentication $\rightarrow$ Home/Discovery $\rightarrow$ Event Details $\rightarrow$ Multi-tier Seat Selection $\rightarrow$ Payment Selection (UPI / Card) $\rightarrow$ Atomic Firestore Booking Transaction $\rightarrow$ Booking Confirmation $\rightarrow$ My Tickets with QR Code.
- **Comprehensive Admin Suite**: Admin Dashboard with live KPI counters (Total Revenue, Tickets Sold, Active Events, Upcoming Events), Event Management (Create, Edit with Multi-Tier Pricing, Delete), Booking Management (Search, Filter by status/date), and User Management (Role toggling & Super Admin protection).
- **Tested & Verified**: All Kotlin sources compile cleanly without errors, and unit test suites execute successfully.

---

## 2. Technology Stack & Specifications

| Component | Specification / Version |
|---|---|
| **Programming Language** | Kotlin 100% (`JavaVersion.VERSION_11` target) |
| **Android SDK** | `compileSdk 37`, `targetSdk 36`, `minSdk 24` |
| **Android Gradle Plugin (AGP)** | `9.3.2` |
| **Build Configuration** | Gradle Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`, `libs.versions.toml`) |
| **UI Design System** | Google Material Design 3 (`Theme.Material3.DayNight.NoActionBar`), MaterialCardView, ShapeableImageView |
| **Architecture** | MVVM / Repository Pattern with Kotlin Coroutines & Flow/Suspend |
| **Authentication** | Firebase Authentication (Email/Password, Google Sign-In with Android Credential Manager) |
| **Database** | Cloud Firestore (Real-time snapshots, multi-field indexes, atomic ACID transactions) |
| **Cloud Storage** | Firebase Cloud Storage (Event banners & media) |
| **Barcode / QR** | ZXing Android Embedded (`com.journeyapps:zxing-android-embedded:4.3.0`) |
| **Image Loading** | Bumptech Glide (`com.github.bumptech.glide:glide:4.16.0`) |

---

## 3. Directory & File Architecture

```
c:/Event_Nest_App/
├── build.gradle.kts                           # Root project Gradle configuration
├── settings.gradle.kts                        # Repository management & project structure
├── gradle.properties                          # Build caching & JVM memory tuning
├── firebase.json                              # Firebase deployment configuration
├── firestore.rules                            # Firestore security rules
├── storage.rules                              # Firebase Storage security rules
├── PROJECT_STATUS.md                          # Project documentation and current status
├── gradle/
│   └── libs.versions.toml                     # Centralized dependency catalog
└── app/
    ├── build.gradle.kts                       # App module build configuration
    ├── google-services.json                   # Firebase client credentials
    └── src/
        ├── test/java/com/example/eventnest/   # Unit test test suites
        │   ├── AdminBookingsFilterTest.kt     # Booking filter helper tests
        │   ├── AdminStatsTest.kt              # Dashboard revenue & ticket math tests
        │   ├── CardValidationTest.kt          # Credit/debit card Luhn & expiration tests
        │   ├── CurrencyUtilsTest.kt           # Currency formatting tests
        │   ├── EventSeatsCalculationTest.kt   # Dynamic ticket tier calculation tests
        │   ├── ManageEventsFilterTest.kt      # Event search & status filter tests
        │   ├── ManageUsersProtectionTest.kt   # Admin safety & role tests
        │   ├── ProfileUtilsTest.kt            # Profile display helpers
        │   └── UpiValidationTest.kt           # UPI VPA format validation tests
        └── main/
            ├── AndroidManifest.xml            # Manifest declaring 17 activities & permissions
            ├── java/com/example/eventnest/
            │   ├── EventNestApplication.kt    # Base Application instance
            │   ├── BaseActivity.kt            # Reusable base activity
            │   ├── SplashActivity.kt          # Animated entry screen & auth router
            │   ├── MainActivity.kt            # Authenticated session router
            │   ├── LoginActivity.kt           # Email/Password & Google Sign-In
            │   ├── RegisterActivity.kt        # User registration & Firestore sync
            │   ├── ForgotPasswordActivity.kt  # Password reset flow
            │   ├── HomeActivity.kt            # Main user discovery screen (Search, Categories, Events)
            │   ├── EventDetailActivity.kt     # Event details, tiers, and booking trigger
            │   ├── BookTicketsActivity.kt     # Tier selection, quantity counter, price calculation
            │   ├── PaymentMethodActivity.kt   # Select UPI, Card, or Demo Checkout
            │   ├── UpiPaymentActivity.kt      # UPI payment form & validation
            │   ├── CardPaymentActivity.kt     # Card payment form & validation
            │   ├── BookingConfirmationActivity.kt # Success screen with ticket summary & CTA
            │   ├── MyTicketsActivity.kt       # User booking history & QR pass modal
            │   ├── ProfileActivity.kt         # User profile, role badges, and settings
            │   ├── AdminDashboardActivity.kt  # Admin metrics & quick management portal
            │   ├── ManageEventsActivity.kt    # Event catalog management (search, filter, delete)
            │   ├── AddEditEventActivity.kt    # Event creation & editing with dynamic ticket tiers
            │   ├── AdminBookingsActivity.kt   # Admin booking logs & confirmation management
            │   ├── ManageUsersActivity.kt     # User list, role elevation & demotion
            │   ├── AuthRepository.kt          # Firebase Auth logic
            │   ├── UserRepository.kt          # Firestore users collection CRUD & role sync
            │   ├── adapters/
            │   │   ├── AdminBookingAdapter.kt # Admin booking item view holder & bindings
            │   │   ├── CategoryAdapter.kt     # Horizontal category filter chips
            │   │   ├── EventAdapter.kt        # Vertical event list cards
            │   │   ├── HomeEventAdapter.kt    # Featured / horizontal event cards
            │   │   ├── ManageEventAdapter.kt  # Admin event list cards with action buttons
            │   │   └── ManageUserAdapter.kt   # Admin user table rows with role switches
            │   ├── models/
            │   │   ├── Booking.kt             # Booking data model (userId, eventId, tier, total)
            │   │   ├── Category.kt            # Category data model (id, name, icon)
            │   │   ├── Event.kt               # Event model with multi-tier ticket support
            │   │   ├── TicketTier.kt          # Individual tier model (name, price, capacity)
            │   │   └── User.kt                # User profile & role model
            │   ├── repositories/
            │   │   ├── AdminRepository.kt     # Real-time dashboard KPI & statistics listener
            │   │   ├── BookingRepository.kt   # Atomic transactional ticket booking engine
            │   │   ├── CategoryRepository.kt  # Category fetching & cache
            │   │   └── EventRepository.kt     # Event CRUD & real-time querying
            │   └── utils/
            │       ├── AdminBookingsFilterHelper.kt
            │       ├── CurrencyUtils.kt
            │       ├── EventSeatHelper.kt
            │       ├── FirestoreExtensions.kt
            │       ├── ManageEventsFilterHelper.kt
            │       └── NavigationUtils.kt
            └── res/
                ├── layout/                    # 20+ responsive Material 3 layout files
                ├── drawable/                  # Vector icons, badge drawables, gradients
                ├── values/                    # Brand tokens: colors, strings, themes
                └── values-night/              # Dark mode theme overrides
```

---

## 4. Implemented Feature Modules

### 4.1 Authentication & Profile Sync
- **Email & Password**: Registration and login with real-time field validation.
- **Google Sign-In**: Uses Android Credential Manager API (`androidx.credentials`) and Google ID option.
- **Firestore User Sync**: Every sign-in/registration guarantees a document in `/users/{uid}` with `role = "user"` (or preserved existing role such as `"admin"`).
- **Password Reset**: Automated password recovery emails via Firebase Auth.
- **Session Gatekeeping**: [SplashActivity](file:///c:/Event_Nest_App/app/src/main/java/com/example/eventnest/SplashActivity.kt) determines if user is logged in, validates their Firestore document, and routes directly to [HomeActivity](file:///c:/Event_Nest_App/app/src/main/java/com/example/eventnest/HomeActivity.kt) or [AdminDashboardActivity](file:///c:/Event_Nest_App/app/src/main/java/com/example/eventnest/AdminDashboardActivity.kt).

### 4.2 Event Discovery & Browsing
- **Dynamic Categories**: Category chip bar with instant filter switching.
- **Search Bar**: Real-time filtering by event title, venue, or description.
- **Event Cards**: Material 3 cards showing banner image, date, time, venue, and lowest starting price.
- **Event Details**: Complete event breakdown with full description, available seats status, organizer details, and dynamic tier pricing options.

### 4.3 Ticketing & Atomic Booking Engine
- **Multi-Tier Ticket Support**: Events can define multiple tiers (e.g. *General Admission*, *VIP*, *VVIP*) with individual prices and available seats.
- **Race Condition Prevention**: `createBooking()` executes inside a **Firestore Transaction** (`runTransaction`) verifying seat availability and decrementing seats atomically to ensure zero overbooking.
- **Payment Integration**:
  - Payment method selection screen ([PaymentMethodActivity](file:///c:/Event_Nest_App/app/src/main/java/com/example/eventnest/PaymentMethodActivity.kt)).
  - UPI Payment screen ([UpiPaymentActivity](file:///c:/Event_Nest_App/app/src/main/java/com/example/eventnest/UpiPaymentActivity.kt)) with VPA validation (`username@bank`).
  - Card Payment screen ([CardPaymentActivity](file:///c:/Event_Nest_App/app/src/main/java/com/example/eventnest/CardPaymentActivity.kt)) with 16-digit formatting, Luhn algorithm check, expiry date and CVV validation.
- **Booking Confirmation**: Displays ticket reference ID, booking details, and instant navigation to view passes.
- **My Tickets**: Displays all past and upcoming user bookings, with integrated ZXing QR code generation for gate check-in.

### 4.4 Admin Operations & Management
- **Live KPI Dashboard**: Real-time Firestore snapshot listeners compute:
  - Total Revenue (sum of all confirmed booking amounts)
  - Tickets Sold (count of booked tickets)
  - Active Events (currently listed events)
  - Upcoming Events (scheduled future events)
- **Manage Events**: View active/inactive events, live search, quick delete dialog with confirmation, and direct navigation to edit.
- **Add / Edit Event**: Full form with title, category selector, venue, date/time pickers, banner image URL, and dynamic multi-tier ticket generator.
- **Admin Bookings**: Filter bookings by status (`Confirmed`, `Pending`, `Cancelled`) and date, with search by booking ID or event name.
- **User Management**: Inspect registered users and toggle roles between `user` and `admin`, with safety guards preventing self-demotion or removal of the master admin.

---

## 5. Firestore Database Structure

```
Firestore Root
│
├── users/ {uid}
│   ├── uid: String
│   ├── name: String
│   ├── email: String
│   ├── role: String ("user" | "admin")
│   └── createdAt: Long (Timestamp millis)
│
├── categories/ {categoryId}
│   ├── categoryId: String
│   ├── categoryName: String
│   └── iconUrl: String
│
├── events/ {eventId}
│   ├── eventId: String
│   ├── eventName: String
│   ├── categoryId: String
│   ├── description: String
│   ├── venue: String
│   ├── date: String
│   ├── time: String
│   ├── bannerImageUrl: String
│   ├── status: String ("active" | "cancelled" | "completed")
│   ├── price: Double (Legacy compatibility)
│   ├── totalSeats: Int (Legacy compatibility)
│   ├── availableSeats: Int (Legacy compatibility)
│   └── tiers: Array<Map>
│       └── [
│             { tierId: String, tierName: String, price: Double, totalSeats: Int, availableSeats: Int }
│           ]
│
└── bookings/ {bookingId}
    ├── bookingId: String
    ├── userId: String
    ├── eventId: String
    ├── eventName: String
    ├── bookingDate: String
    ├── numberOfTickets: Int
    ├── totalAmount: Double
    ├── status: String ("confirmed" | "cancelled" | "pending")
    ├── tierId: String
    └── tierName: String
```

---

## 6. Build and Verification Health

- **Kotlin Compilation (`./gradlew compileDebugKotlin`)**:
  - Status: **PASSED** (0 errors, 0 warnings blocking compilation).
- **Unit Testing (`./gradlew testDebugUnitTest`)**:
  - Status: **PASSED** (All 10 unit test files executed and passed).
- **Gradle Configuration Cache**: Enabled and functioning properly.
- **Dependencies**: Centralized in `libs.versions.toml`.

---

## 7. Next Potential Roadmap Enhancements

1. **Push Notifications**: Firebase Cloud Messaging (FCM) for event reminders and ticket confirmation receipts.
2. **Camera QR Scanner for Admins**: Utilizing the existing ZXing dependency to scan and validate attendee QR codes at event entry doors.
3. **Live Geolocation & Maps**: Integration with Google Maps SDK for venue navigation.
4. **Direct Image Uploads**: Integrating Camera/Gallery image picker with direct upload to Firebase Storage bucket.
