# Medium/Connect App - Architecture Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture Pattern](#architecture-pattern)
3. [Project Structure](#project-structure)
4. [Firebase Integration](#firebase-integration)
5. [Room Database Architecture](#room-database-architecture)
6. [Notification System](#notification-system)
7. [Data Flow & Observers](#data-flow--observers)
8. [LiveData & MutableLiveData Usage](#livedata--mutablelivedata-usage)
9. [Kotlin Flows](#kotlin-flows)
10. [Repository Pattern](#repository-pattern)
11. [Data Models](#data-models)
12. [Key Components](#key-components)

---

## Overview

This is a social media Android application built with Kotlin that implements a modern Android architecture using **MVVM (Model-View-ViewModel)** pattern, **Repository pattern**, **Room Database** for local caching, and **Firebase** as the backend. The app supports real-time updates through Firebase Firestore listeners and LiveData/Flow observers.

### Tech Stack
- **Language**: Kotlin
- **UI**: ViewBinding, Navigation Component
- **Architecture**: MVVM + Repository Pattern
- **Database**: Room (local caching), Firebase Firestore (remote)
- **Authentication**: Firebase Auth
- **Push Notifications**: Firebase Cloud Messaging (FCM)
- **Image Handling**: Glide, Cloudinary
- **Async Operations**: Kotlin Coroutines, Flow
- **Reactive UI**: LiveData, Flow

---

## Architecture Pattern

### MVVM (Model-View-ViewModel)

```
┌─────────────┐
│    View     │  (Fragment/Activity)
│  (UI Layer) │
└──────┬──────┘
       │ observes
       ▼
┌─────────────┐
│  ViewModel  │  (Business Logic)
└──────┬──────┘
       │ calls
       ▼
┌─────────────┐
│ Repository  │  (Data Layer)
└──────┬──────┘
       │
       ├─────────────┐
       ▼             ▼
┌──────────┐  ┌──────────────┐
│   Room   │  │   Firebase   │
│ Database │  │  Firestore   │
└──────────┘  └──────────────┘
```

#### Key Principles:
1. **Separation of Concerns**: UI, business logic, and data layers are separated
2. **Single Source of Truth**: Room database acts as the local cache
3. **Unidirectional Data Flow**: Data flows from repositories → ViewModels → Views
4. **Lifecycle Awareness**: ViewModels survive configuration changes

---

## Project Structure

```
com.connect.medium/
├── data/
│   ├── local/              # Room Database
│   │   ├── AppDatabase.kt
│   │   ├── Converters.kt
│   │   ├── dao/            # Data Access Objects
│   │   │   ├── UserDao.kt
│   │   │   ├── PostDao.kt
│   │   │   ├── NotificationDao.kt
│   │   │   └── FollowDao.kt
│   │   └── entity/         # Room Entities
│   │       ├── UserEntity.kt
│   │       ├── PostEntity.kt
│   │       ├── NotificationEntity.kt
│   │       └── FollowEntity.kt
│   ├── remote/             # Firebase Data Sources
│   │   ├── FirestoreDataSource.kt
│   │   └── CloudinaryDataSource.kt
│   ├── repository/         # Repository Pattern
│   │   ├── AuthRepository.kt
│   │   ├── UserRepository.kt
│   │   ├── PostRepository.kt
│   │   └── NotificationRepository.kt
│   └── model/              # Domain Models
│       ├── User.kt
│       ├── Post.kt
│       ├── Comment.kt
│       └── Notification.kt
├── ui/
│   ├── auth/               # Authentication UI
│   ├── main/               # Main Activity & Fragments
│   │   ├── fragments/
│   │   │   ├── home/
│   │   │   ├── search/
│   │   │   ├── profile/
│   │   │   ├── notifications/
│   │   │   └── create/
│   │   └── adapters/       # RecyclerView Adapters
│   └── MainActivity.kt
├── service/
│   └── FCMService.kt       # Firebase Cloud Messaging
├── utils/
│   ├── Extensions.kt       # Entity ↔ Model Converters
│   ├── Resource.kt         # Result Wrapper
│   ├── FCMHelper.kt
│   └── FCMSender.kt
└── ConnectApplication.kt   # Application Class
```

---

## Firebase Integration

### 1. Firebase Firestore (Backend Database)

Firebase Firestore is the **primary backend database** that stores all user data, posts, comments, and notifications.

#### Firestore Collections Structure:
```
users/
  └── {uid}/
      ├── username
      ├── displayName
      ├── profileImageUrl
      ├── fcmToken
      ├── followerCount
      ├── followingCount
      ├── followers/
      └── following/

posts/
  └── {postId}/
      ├── authorUid
      ├── caption
      ├── mediaUrls
      ├── likeCount
      ├── commentCount
      ├── createdAt
      ├── likes/
      │   └── {uid}
      └── comments/
          └── {commentId}/

notifications/
  └── {notificationId}/
      ├── toUid
      ├── fromUid
      ├── type
      ├── postId
      ├── read
      └── createdAt
```

#### FirestoreDataSource.kt

The `FirestoreDataSource` class handles all Firebase Firestore operations:

**Key Features:**
- **Real-time Listeners**: Uses Firestore snapshots for real-time updates
- **Batch Operations**: Atomic writes for multiple updates
- **callbackFlow**: Converts Firestore listeners to Kotlin Flows

**Example - Observing Posts:**
```kotlin
fun observeFeedPosts(): Flow<List<Post>> = callbackFlow {
    val listener = firestore.collection(Constants.COLLECTION_POSTS)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(50)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val posts = snapshot?.toObjects(Post::class.java) ?: emptyList()
            trySend(posts)
        }
    awaitClose { listener.remove() }
}
```

**How it Works:**
1. Creates a Firestore query with `.orderBy()` and `.limit()`
2. Attaches a snapshot listener that fires on data changes
3. Converts Firestore data to `Post` objects using `.toObjects()`
4. Emits data through `callbackFlow` (Kotlin Flow)
5. Removes listener when Flow is cancelled (in `awaitClose`)

### 2. Firebase Authentication

**AuthRepository.kt** manages authentication:

```kotlin
class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    
    suspend fun login(email: String, password: String): Resource<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Resource.Success(result.user!!)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Login failed")
        }
    }
    
    fun getCurrentUser(): FirebaseUser? = auth.currentUser
}
```

**Features:**
- Uses `await()` extension for suspending function
- Returns `Resource<T>` sealed class for state handling
- Provides current user access throughout the app

### 3. Firebase Cloud Messaging (FCM)

Handles push notifications when app is in background.

---

## Room Database Architecture

### Why Room?

Room provides **local caching** for offline support and faster data access. When data is fetched from Firebase, it's automatically cached in Room.

### Database Initialization

**AppDatabase.kt:**
```kotlin
@Database(
    entities = [
        UserEntity::class,
        PostEntity::class,
        NotificationEntity::class,
        FollowEntity::class
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao
    abstract fun notificationDao(): NotificationDao
    abstract fun followDao(): FollowDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
            }
        }
    }
}
```

**Key Components:**

1. **@Database Annotation**: Declares entities and database version
2. **Singleton Pattern**: Ensures only one database instance exists
3. **@TypeConverters**: Handles complex data types (lists, enums)
4. **fallbackToDestructiveMigration()**: Recreates DB on schema changes
5. **DAOs**: Abstract functions return DAO instances

### Type Converters

**Converters.kt:**
```kotlin
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return gson.fromJson(value, Array<String>::class.java).toList()
    }
}
```

**Purpose:** Converts complex types (Lists) to String for storage in SQLite.

### Room Entities

**NotificationEntity.kt:**
```kotlin
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val notificationId: String,
    val toUid: String,
    val fromUid: String,
    val fromUsername: String,
    val fromProfileImageUrl: String,
    val type: String,
    val postId: String,
    val read: Boolean,
    val createdAt: Long
)
```

**Key Features:**
- `@Entity`: Marks class as a Room table
- `@PrimaryKey`: Defines primary key
- Immutable data class (use `data class` for auto-generated methods)

### Data Access Objects (DAOs)

**NotificationDao.kt:**
```kotlin
@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications WHERE toUid = :uid ORDER BY createdAt DESC")
    fun getNotificationsForUser(uid: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE toUid = :uid AND read = 0")
    fun getUnreadCount(uid: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Transaction
    suspend fun upsertNotifications(notifications: List<NotificationEntity>) {
        notifications.forEach { notification ->
            val existing = getNotificationById(notification.notificationId)
            if (existing == null) {
                insertNotification(notification)
            }
        }
    }

    @Query("UPDATE notifications SET read = 1 WHERE toUid = :uid")
    suspend fun markAllAsRead(uid: String)
}
```

**Key Features:**
1. **@Query**: Raw SQL queries with parameter binding
2. **Flow<T>**: Returns Flow for reactive updates
3. **suspend**: Coroutine-aware database operations
4. **@Transaction**: Ensures atomic operations
5. **OnConflictStrategy.REPLACE**: Updates existing records

### Entity ↔ Model Mapping

**Extensions.kt:**
```kotlin
// Model → Entity (for saving to Room)
fun Notification.toEntity(): NotificationEntity = NotificationEntity(
    notificationId, toUid, fromUid, fromUsername,
    fromProfileImageUrl, type.name, postId, read, createdAt
)

// Entity → Model (for displaying in UI)
fun NotificationEntity.toModel(): Notification = Notification(
    notificationId, toUid, fromUid, fromUsername,
    fromProfileImageUrl, NotificationType.valueOf(type), postId, read, createdAt
)
```

**Why Separate Entities and Models?**
- **Entities**: Optimized for database storage (simple types)
- **Models**: Rich domain objects with enums, sealed classes, etc.
- **Separation**: Keeps database schema independent from business logic

---

## Notification System

### Complete Notification Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    Notification Flow                        │
└─────────────────────────────────────────────────────────────┘

1. User Action (Like/Comment/Follow)
        ↓
2. Repository sends notification to Firestore
        ↓
3. FCMSender.sendNotification() sends push via HTTP API
        ↓
4. Target user's device receives FCM message
        ↓
5. FCMService.onMessageReceived() triggered
        ↓
6. Creates Android Notification with PendingIntent
        ↓
7. User taps notification → Intent extras passed to MainActivity
        ↓
8. MainActivity.handleNotificationIntent() navigates to content
```

### 1. Sending Notifications

**PostRepository.kt (Example: Like Notification):**
```kotlin
suspend fun likePost(postId: String, uid: String, postAuthorUid: String, fromUser: User): Resource<Unit> {
    return try {
        // 1. Update Firestore like count
        firestoreDataSource.likePost(postId, uid)

        // 2. Create notification object
        if (uid != postAuthorUid) {
            val notification = Notification(
                notificationId = UUID.randomUUID().toString(),
                toUid = postAuthorUid,
                fromUid = uid,
                fromUsername = fromUser.username,
                fromProfileImageUrl = fromUser.profileImageUrl,
                type = NotificationType.LIKE,
                postId = postId,
                createdAt = System.currentTimeMillis()
            )
            
            // 3. Save to Firestore notifications collection
            firestoreDataSource.sendNotification(notification)

            // 4. Get target user's FCM token
            val targetToken = firestoreDataSource.getUserFcmToken(postAuthorUid)
            
            // 5. Send push notification
            if (!targetToken.isNullOrEmpty()) {
                FCMSender.sendNotification(
                    targetToken = targetToken,
                    title = fromUser.username,
                    body = "liked your post",
                    type = "LIKE",
                    postId = postId,
                    fromUid = uid
                )
            }
        }

        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to like post")
    }
}
```

### 2. FCM Token Management

**FCMHelper.kt:**
```kotlin
object FCMHelper {
    fun saveTokenToFirestore(uid: String) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .update("fcmToken", token)
                        .await()
                } catch (e: Exception) {
                    Log.e("FCMHelper", "Failed to save token: ${e.message}")
                }
            }
        }
    }
}
```

**When is FCM Token Saved?**
- On user login
- On token refresh (in FCMService.onNewToken())
- Stored in Firestore users collection

### 3. FCM Service (Receiving Notifications)

**FCMService.kt:**
```kotlin
class FCMService : FirebaseMessagingService() {

    // Called when FCM token is refreshed
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .update("fcmToken", token)
                    .await()
            } catch (e: Exception) {
                Log.e("FCMService", "Failed to update token: ${e.message}")
            }
        }
    }

    // Called when notification is received while app is in foreground
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        // Extract notification data
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: return
        val type = message.data["type"] ?: ""
        val postId = message.data["postId"] ?: ""
        val fromUid = message.data["fromUid"] ?: ""
        
        // Show notification
        showNotification(title, body, type, postId, fromUid)
    }

    private fun showNotification(title: String, body: String, type: String, postId: String, fromUid: String) {
        val channelId = "connect_notifications"

        // Create intent with extras
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", type)
            putExtra("notification_post_id", postId)
            putExtra("notification_from_uid", fromUid)
        }

        // Create pending intent
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Show notification
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
```

**Key Concepts:**
- **onMessageReceived()**: Called when app is in foreground
- **PendingIntent**: Launches MainActivity with intent extras when notification is tapped
- **Intent Extras**: Pass `type`, `postId`, `fromUid` for navigation

### 4. Handling Notification Intent

**MainActivity.kt:**
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // ... setup code ...
    
    handleNotificationIntent(intent)
}

override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleNotificationIntent(intent)
}

private fun handleNotificationIntent(intent: Intent) {
    val type = intent.getStringExtra("notification_type") ?: return
    val postId = intent.getStringExtra("notification_post_id") ?: ""
    val fromUid = intent.getStringExtra("notification_from_uid") ?: ""

    binding.root.post {
        when (type) {
            "FOLLOW" -> {
                if (fromUid.isNotEmpty()) {
                    navController.navigate(
                        R.id.userProfileFragment,
                        Bundle().apply { putString("uid", fromUid) }
                    )
                }
            }
            "LIKE", "COMMENT" -> {
                if (postId.isNotEmpty()) {
                    navController.navigate(
                        R.id.viewPostFragment,
                        Bundle().apply { putString("postId", postId) }
                    )
                }
            }
        }
    }
}
```

**Flow:**
1. User taps notification → MainActivity launched with intent extras
2. Extract `type`, `postId`, `fromUid` from intent
3. Use Navigation Component to navigate to appropriate screen
4. `binding.root.post {}`: Ensures navigation happens after view is ready

### 5. FCM HTTP API (Server-Side Sending)

**FCMSender.kt:**
```kotlin
object FCMSender {
    private const val FCM_URL = "https://fcm.googleapis.com/v1/projects/medium-5e53e/messages:send"
    
    suspend fun sendNotification(
        targetToken: String,
        title: String,
        body: String,
        type: String,
        postId: String = "",
        fromUid: String = ""
    ) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Get OAuth2 access token
                val accessToken = getAccessToken()
                
                // 2. Build FCM payload
                val json = JSONObject().apply {
                    put("message", JSONObject().apply {
                        put("token", targetToken)
                        put("notification", JSONObject().apply {
                            put("title", title)
                            put("body", body)
                        })
                        put("data", JSONObject().apply {
                            put("type", type)
                            put("postId", postId)
                            put("fromUid", fromUid)
                        })
                        put("android", JSONObject().apply {
                            put("priority", "high")
                        })
                    })
                }

                // 3. Send HTTP POST request
                val url = URL(FCM_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "POST"
                    setRequestProperty("Authorization", "Bearer $accessToken")
                    setRequestProperty("Content-Type", "application/json; UTF-8")
                    doOutput = true
                    outputStream.write(json.toString().toByteArray(Charsets.UTF_8))
                }

                val responseCode = conn.responseCode
                Log.d("FCMSender", "Response code: $responseCode")
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("FCMSender", "Exception: ${e.message}", e)
            }
        }
    }

    private fun getAccessToken(): String? {
        return try {
            val stream = ConnectApplication.instance.assets.open("service_account.json")
            val credentials = GoogleCredentials
                .fromStream(stream)
                .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
            credentials.refreshIfExpired()
            credentials.accessToken.tokenValue
        } catch (e: Exception) {
            Log.e("FCMSender", "Failed to get access token: ${e.message}", e)
            null
        }
    }
}
```

**Key Points:**
- Uses **Firebase Admin SDK** credentials (service_account.json)
- Gets **OAuth2 access token** for authentication
- Sends **HTTP POST** to FCM v1 API
- Includes **notification** (for display) and **data** (for custom handling)

### 6. Notification Permission Handling

**MainActivity.kt:**
```kotlin
private val notificationPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (isGranted) {
                Log.d("MainActivity", "Notification permission granted")
            } else {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    showGoToSettingsDialog()
                }
            }
        }
    }

private fun requestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    when {
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED -> {
            // Permission already granted
        }
        shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
            showNotificationRationale()
        }
        else -> {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
```

**Android 13+ Requirement:**
- Apps must request `POST_NOTIFICATIONS` permission
- Handled in MainActivity onCreate()

### 7. Real-time Notification Updates

**NotificationRepository.kt:**
```kotlin
fun observeNotifications(uid: String): Flow<List<Notification>> {
    return firestoreDataSource.observeNotifications(uid)
        .onEach { notifications ->
            // Cache to Room for offline access
            notificationDao.upsertNotifications(notifications.map { it.toEntity() })
        }
}
```

**Flow:**
1. Firestore listener emits notifications in real-time
2. `.onEach {}`: Caches notifications to Room database
3. ViewModel collects Flow and updates LiveData
4. Fragment observes LiveData and updates UI

### 8. Notification Badge

**MainActivity.kt:**
```kotlin
private fun setupNotificationBadge(){
    notificationsViewModel = ViewModelProvider(
        this,
        NotificationViewModelFactory(application)
    )[NotificationsViewModel::class.java]

    notificationsViewModel.unreadCount.observe(this) { count ->
        val badge = binding.bottomNav.getOrCreateBadge(R.id.notificationsFragment)
        if (count > 0) {
            badge.isVisible = true
            badge.number = count
        } else {
            badge.isVisible = false
        }
    }
}
```

**How it Works:**
1. `unreadCount` is a Flow from Room DAO
2. Converted to LiveData using `.asLiveData()`
3. Observes Room database for unread notifications
4. Updates BottomNavigationView badge in real-time

---

## Data Flow & Observers

### Complete Data Flow Architecture

```
┌────────────────────────────────────────────────────────────┐
│                    Data Flow Diagram                       │
└────────────────────────────────────────────────────────────┘

Firebase Firestore (Source of Truth)
        │
        │ Real-time Snapshot Listener
        ↓
    callbackFlow { ... }
        │
        │ Kotlin Flow<List<Post>>
        ↓
    Repository.observePosts()
        │
        │ .onEach { cache to Room }
        ↓
    Room Database (Local Cache)
        │
        │ Flow<List<PostEntity>>
        ↓
    ViewModel
        │
        │ .collect { update LiveData }
        ↓
    LiveData<Resource<List<Post>>>
        │
        │ .observe(viewLifecycleOwner)
        ↓
    Fragment (UI Update)
```

### Example: Feed Posts Flow

**1. FirestoreDataSource (Data Source)**
```kotlin
fun observeFeedPosts(): Flow<List<Post>> = callbackFlow {
    val listener = firestore.collection(Constants.COLLECTION_POSTS)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(50)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val posts = snapshot?.toObjects(Post::class.java) ?: emptyList()
            trySend(posts) // Emit to Flow
        }
    awaitClose { listener.remove() } // Cleanup
}
```

**2. PostRepository (Caching Layer)**
```kotlin
fun observeFeedPosts(): Flow<List<Post>> {
    return firestoreDataSource.observeFeedPosts()
        .onEach { posts ->
            // Cache to Room every time Firestore updates
            postDao.insertPosts(posts.map { it.toEntity() })
        }
}
```

**3. HomeViewModel (Business Logic)**
```kotlin
private val _postsState = MutableLiveData<Resource<List<Post>>>()
val postsState: LiveData<Resource<List<Post>>> = _postsState

init {
    loadFeed()
}

fun loadFeed() {
    _postsState.value = Resource.Loading
    viewModelScope.launch {
        try {
            val (posts, lastDoc) = postRepository.getFeedPostsPaginated(limit = 5)
            allPosts.addAll(posts)
            lastDocument = lastDoc
            _postsState.value = Resource.Success(allPosts.toList())
        } catch (e: Exception) {
            _postsState.value = Resource.Error(e.message ?: "Failed to load feed")
        }
    }
}
```

**4. HomeFragment (UI Layer)**
```kotlin
private fun observeViewModel() {
    viewModel.postsState.observe(viewLifecycleOwner) { resource ->
        when (resource) {
            is Resource.Loading -> {
                showShimmer(true)
            }
            is Resource.Success -> {
                postAdapter.submitList(resource.data)
                binding.rvFeed.visibility = View.VISIBLE
            }
            is Resource.Error -> {
                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
```

### Key Observer Patterns Used

1. **Flow.collect()**: Collects Flow emissions in coroutines
   ```kotlin
   viewModelScope.launch {
       repository.observeUser(uid).collect { user ->
           _currentUser.value = user
       }
   }
   ```

2. **LiveData.observe()**: Observes LiveData changes lifecycle-aware
   ```kotlin
   viewModel.postsState.observe(viewLifecycleOwner) { posts ->
       updateUI(posts)
   }
   ```

3. **Flow.asLiveData()**: Converts Flow to LiveData
   ```kotlin
   val unreadCount: LiveData<Int> = notificationRepository
       .getUnreadCount(currentUid)
       .asLiveData()
   ```

4. **callbackFlow**: Bridges callback-based APIs to Flow
   ```kotlin
   fun observePosts(): Flow<List<Post>> = callbackFlow {
       val listener = firestore.addSnapshotListener { snapshot, _ ->
           trySend(snapshot?.toObjects(Post::class.java) ?: emptyList())
       }
       awaitClose { listener.remove() }
   }
   ```

---

## LiveData & MutableLiveData Usage

### What is LiveData?

LiveData is an **observable data holder** that is **lifecycle-aware**. It ensures:
- UI updates only when Fragment/Activity is in active state
- No memory leaks (automatically cleans up observers)
- No crashes due to stopped activities

### MutableLiveData vs LiveData

```kotlin
// Private MutableLiveData - Can be modified
private val _postsState = MutableLiveData<Resource<List<Post>>>()

// Public LiveData - Read-only exposure
val postsState: LiveData<Resource<List<Post>>> = _postsState
```

**Why Two Variables?**
- **Encapsulation**: ViewModel can modify `_postsState`, but Fragments can only observe `postsState`
- **Prevents accidental modification** from UI layer

### LiveData Patterns in the App

#### 1. Resource Wrapper Pattern

**Resource.kt:**
```kotlin
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}
```

**Usage in ViewModel:**
```kotlin
private val _postsState = MutableLiveData<Resource<List<Post>>>()
val postsState: LiveData<Resource<List<Post>>> = _postsState

fun loadPosts() {
    _postsState.value = Resource.Loading
    viewModelScope.launch {
        try {
            val posts = repository.getPosts()
            _postsState.value = Resource.Success(posts)
        } catch (e: Exception) {
            _postsState.value = Resource.Error(e.message ?: "Unknown error")
        }
    }
}
```

**Benefits:**
- Handles loading, success, and error states
- UI can react to each state appropriately

#### 2. Optimistic UI Updates

**HomeViewModel.kt:**
```kotlin
private val _likedPostIds = MutableLiveData<Set<String>>()
val likedPostIds: LiveData<Set<String>> = _likedPostIds

private val localLikedPosts = mutableSetOf<String>()

fun toggleLike(post: Post) {
    viewModelScope.launch {
        val isLiked = localLikedPosts.contains(post.postId)
        
        // Optimistic update - Update UI immediately
        if (isLiked) localLikedPosts.remove(post.postId)
        else localLikedPosts.add(post.postId)
        _likedPostIds.value = localLikedPosts.toSet()

        // Perform network request
        val result = if (isLiked) {
            postRepository.unlikePost(post.postId, currentUid)
        } else {
            postRepository.likePost(post.postId, currentUid, post.authorUid, currentUser)
        }

        // Rollback on error
        if (result is Resource.Error) {
            if (isLiked) localLikedPosts.add(post.postId)
            else localLikedPosts.remove(post.postId)
            _likedPostIds.value = localLikedPosts.toSet()
        }
    }
}
```

**Benefits:**
- Instant UI feedback (no waiting for network)
- Rollback on failure

#### 3. Multiple LiveData Observations

**HomeFragment.kt:**
```kotlin
private fun observeViewModel() {
    // Observe posts state
    viewModel.postsState.observe(viewLifecycleOwner) { resource ->
        when (resource) {
            is Resource.Loading -> showShimmer(true)
            is Resource.Success -> {
                postAdapter.submitList(resource.data)
                viewModel.checkLikedPosts(resource.data.map { it.postId })
            }
            is Resource.Error -> showError(resource.message)
        }
    }

    // Observe liked posts
    viewModel.likedPostIds.observe(viewLifecycleOwner) { likedIds ->
        postAdapter.setLikedPosts(likedIds)
    }

    // Observe pagination state
    viewModel.paginationState.observe(viewLifecycleOwner) { resource ->
        when (resource) {
            is Resource.Loading -> postAdapter.showLoadingFooter(true)
            is Resource.Success -> postAdapter.showLoadingFooter(false)
            is Resource.Error -> showError(resource.message)
        }
    }
}
```

**Benefits:**
- Each concern has its own LiveData
- UI reacts independently to each state

---

## Kotlin Flows

### What is Flow?

Flow is a **cold asynchronous stream** that emits values sequentially. Unlike LiveData, Flow:
- Not lifecycle-aware (needs `.asLiveData()` or manual lifecycle handling)
- More powerful operators (map, filter, combine, etc.)
- Ideal for repository → ViewModel communication

### Flow Patterns in the App

#### 1. callbackFlow (Firebase Listeners → Flow)

**FirestoreDataSource.kt:**
```kotlin
fun observeComments(postId: String): Flow<List<Comment>> = callbackFlow {
    val listener = firestore.collection(Constants.COLLECTION_POSTS)
        .document(postId)
        .collection(Constants.COLLECTION_COMMENTS)
        .orderBy("createdAt", Query.Direction.ASCENDING)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val comments = snapshot?.toObjects(Comment::class.java) ?: emptyList()
            trySend(comments)
        }
    awaitClose { listener.remove() }
}
```

**How it Works:**
- `callbackFlow`: Creates a Flow from callback-based API
- `trySend()`: Emits values to the Flow
- `awaitClose`: Cleanup when Flow is cancelled

#### 2. Room DAOs Return Flows

**NotificationDao.kt:**
```kotlin
@Query("SELECT * FROM notifications WHERE toUid = :uid ORDER BY createdAt DESC")
fun getNotificationsForUser(uid: String): Flow<List<NotificationEntity>>

@Query("SELECT COUNT(*) FROM notifications WHERE toUid = :uid AND read = 0")
fun getUnreadCount(uid: String): Flow<Int>
```

**Benefits:**
- Room automatically updates Flow when database changes
- No manual invalidation needed

#### 3. Flow Operators in Repository

**NotificationRepository.kt:**
```kotlin
fun observeNotifications(uid: String): Flow<List<Notification>> {
    return firestoreDataSource.observeNotifications(uid)
        .onEach { notifications ->
            // Side effect: Cache to Room
            notificationDao.upsertNotifications(notifications.map { it.toEntity() })
        }
}

fun getCachedNotifications(uid: String): Flow<List<Notification>> {
    return notificationDao.getNotificationsForUser(uid)
        .map { list -> list.map { it.toModel() } }
}
```

**Operators Used:**
- `.onEach {}`: Performs side effect on each emission
- `.map {}`: Transforms each element

#### 4. Flow → LiveData Conversion

**NotificationsViewModel.kt:**
```kotlin
val unreadCount: LiveData<Int> = notificationRepository
    .getUnreadCount(currentUid)
    .asLiveData()
```

**Why Convert?**
- LiveData is lifecycle-aware (safe for UI observation)
- Flow needs manual lifecycle handling in fragments

#### 5. Collecting Flows in ViewModel

**NotificationsViewModel.kt:**
```kotlin
private val _notificationsState = MutableLiveData<Resource<List<Notification>>>()
val notificationsState: LiveData<Resource<List<Notification>>> = _notificationsState

fun loadNotifications() {
    _notificationsState.value = Resource.Loading
    viewModelScope.launch {
        notificationRepository.observeNotifications(currentUid)
            .collect { notifications ->
                _notificationsState.value = Resource.Success(notifications)
            }
    }
}
```

**Key Points:**
- `.collect {}`: Terminal operator that collects Flow emissions
- `viewModelScope.launch`: Cancels collection when ViewModel is cleared
- Converts Flow to LiveData for UI observation

---

## Repository Pattern

### Why Repository?

The Repository pattern provides a **clean API** for data access, abstracting:
- Data sources (Room, Firestore, REST APIs)
- Caching logic
- Error handling
- Data synchronization

### Repository Structure

```kotlin
class PostRepository(
    private val firestoreDataSource: FirestoreDataSource,
    private val postDao: PostDao
) {
    // Expose Flow from Firestore with Room caching
    fun observeFeedPosts(): Flow<List<Post>> {
        return firestoreDataSource.observeFeedPosts()
            .onEach { posts ->
                postDao.insertPosts(posts.map { it.toEntity() })
            }
    }
    
    // One-time fetch with error handling
    suspend fun createPost(post: Post): Resource<Unit> {
        return try {
            firestoreDataSource.createPost(post)
            postDao.insertPost(post.toEntity())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create post")
        }
    }
}
```

### Repository Responsibilities

1. **Data Source Coordination**: Decides when to use cache vs. network
2. **Caching Strategy**: Writes to Room after Firestore fetch
3. **Error Handling**: Wraps exceptions in `Resource.Error`
4. **Business Logic**: Sends notifications, updates counts, etc.

### Example: Post Like with Notification

**PostRepository.kt:**
```kotlin
suspend fun likePost(postId: String, uid: String, postAuthorUid: String, fromUser: User): Resource<Unit> {
    return try {
        // 1. Update Firestore
        firestoreDataSource.likePost(postId, uid)

        // 2. Send notification (only if liking someone else's post)
        if (uid != postAuthorUid) {
            val notification = Notification(
                notificationId = UUID.randomUUID().toString(),
                toUid = postAuthorUid,
                fromUid = uid,
                fromUsername = fromUser.username,
                fromProfileImageUrl = fromUser.profileImageUrl,
                type = NotificationType.LIKE,
                postId = postId,
                createdAt = System.currentTimeMillis()
            )
            firestoreDataSource.sendNotification(notification)

            // 3. Send FCM push notification
            val targetToken = firestoreDataSource.getUserFcmToken(postAuthorUid)
            if (!targetToken.isNullOrEmpty()) {
                FCMSender.sendNotification(
                    targetToken = targetToken,
                    title = fromUser.username,
                    body = "liked your post",
                    type = "LIKE",
                    postId = postId,
                    fromUid = uid
                )
            }
        }

        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to like post")
    }
}
```

**Benefits:**
- ViewModel doesn't know about Firestore, Room, or FCM
- Easy to test (mock repository)
- Centralized business logic

---

## Data Models

### Domain Models vs Entities

**Domain Model (Post.kt):**
```kotlin
data class Post(
    val postId: String = "",
    val authorUid: String = "",
    val authorUsername: String = "",
    val authorProfileImageUrl: String = "",
    val mediaUrls: List<String> = emptyList(),
    val mediaTypes: List<String> = emptyList(),
    val caption: String = "",
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
```

**Room Entity (PostEntity.kt):**
```kotlin
@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val postId: String,
    val authorUid: String,
    val authorUsername: String,
    val authorProfileImageUrl: String,
    val mediaUrls: String, // JSON string
    val mediaTypes: String, // JSON string
    val caption: String,
    val likeCount: Int,
    val commentCount: Int,
    val createdAt: Long
)
```

**Key Differences:**
- **Domain Model**: Uses `List<String>` for collections
- **Entity**: Uses `String` (JSON) for Room storage
- **Conversion**: Extension functions in `Extensions.kt`

### Type Conversion Example

**Extensions.kt:**
```kotlin
fun Post.toEntity(): PostEntity = PostEntity(
    postId = postId,
    authorUid = authorUid,
    authorUsername = authorUsername,
    authorProfileImageUrl = authorProfileImageUrl,
    mediaUrls = Gson().toJson(mediaUrls), // List → JSON String
    mediaTypes = Gson().toJson(mediaTypes),
    caption = caption,
    likeCount = likeCount,
    commentCount = commentCount,
    createdAt = createdAt
)

fun PostEntity.toModel(): Post = Post(
    postId = postId,
    authorUid = authorUid,
    authorUsername = authorUsername,
    authorProfileImageUrl = authorProfileImageUrl,
    mediaUrls = Gson().fromJson(mediaUrls, Array<String>::class.java).toList(), // JSON → List
    mediaTypes = Gson().fromJson(mediaTypes, Array<String>::class.java).toList(),
    caption = caption,
    likeCount = likeCount,
    commentCount = commentCount,
    createdAt = createdAt
)
```

---

## Key Components

### 1. ConnectApplication

**ConnectApplication.kt:**
```kotlin
class ConnectApplication : Application() {
    companion object {
        lateinit var instance: ConnectApplication
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize theme from DataStore
        val isDark = runBlocking(Dispatchers.IO) {
            ThemePreferences.getDarkMode(this@ConnectApplication).first()
        }
        
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        
        // Initialize Glide memory cache
        Glide.get(this).apply {
            setMemoryCategory(MemoryCategory.LOW)
        }
    }
}
```

**Purpose:**
- Global application state
- Initialize libraries (Glide, theme)
- Provide application context globally

### 2. ViewModelFactory

**HomeViewModelFactory.kt:**
```kotlin
class HomeViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): ViewModel {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

**Usage in Fragment:**
```kotlin
private val viewModel: HomeViewModel by viewModels {
    HomeViewModelFactory(requireActivity().application)
}
```

**Why Needed?**
- ViewModels with constructor parameters need custom factories
- Passes dependencies (Application, Repository, etc.)

### 3. Navigation Component

**MainActivity.kt:**
```kotlin
val navHostFragment = supportFragmentManager
    .findFragmentById(R.id.main_nav_host) as NavHostFragment
navController = navHostFragment.navController

binding.bottomNav.setupWithNavController(navController)
```

**Safe Args:**
```kotlin
val action = HomeFragmentDirections.actionHomeToComments(
    postId = post.postId,
    postAuthorUid = post.authorUid
)
findNavController().navigate(action)
```

**Benefits:**
- Type-safe argument passing
- Handles back stack automatically
- Deep link support for notifications

---

## Summary

### Data Flow Summary

```
User Interaction
    ↓
Fragment calls ViewModel method
    ↓
ViewModel launches coroutine
    ↓
Repository fetches from Firestore
    ↓
Repository caches to Room (onEach)
    ↓
Flow emits to ViewModel
    ↓
ViewModel updates LiveData
    ↓
Fragment observes LiveData
    ↓
UI updates
```

### Key Architectural Decisions

1. **Firebase as Primary Database**: Real-time updates, scalability
2. **Room as Cache**: Offline support, faster reads
3. **Repository Pattern**: Clean separation, testability
4. **MVVM**: Lifecycle-aware, reactive UI
5. **Flow + LiveData**: Reactive data streams with lifecycle safety
6. **FCM for Notifications**: Real-time push notifications
7. **Coroutines**: Async operations without callback hell

### Real-time Update Mechanisms

1. **Firestore Snapshot Listeners**: Emit on data change
2. **Room Flows**: Emit on database change
3. **LiveData**: Lifecycle-aware observations
4. **FCM**: Push notifications to offline users

This architecture ensures:
- ✅ Real-time updates across all screens
- ✅ Offline-first with Room caching
- ✅ Clean separation of concerns
- ✅ Testable components
- ✅ No memory leaks (lifecycle awareness)
- ✅ Scalable and maintainable codebase

