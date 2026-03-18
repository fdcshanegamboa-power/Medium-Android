package com.connect.medium.data.remote

import com.connect.medium.data.model.*
import com.connect.medium.utils.Constants
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreDataSource {

    private val firestore = FirebaseFirestore.getInstance()

    // ─── User ───────────────────────────────────────────

    suspend fun createUser(user: User) {
        firestore.collection(Constants.COLLECTION_USERS)
            .document(user.uid)
            .set(user)
            .await()
    }

    suspend fun getUser(uid: String): User? {
        return firestore.collection(Constants.COLLECTION_USERS)
            .document(uid)
            .get()
            .await()
            .toObject(User::class.java)
    }

    suspend fun updateUser(uid: String, fields: Map<String, Any>) {
        firestore.collection(Constants.COLLECTION_USERS)
            .document(uid)
            .update(fields)
            .await()
    }

    fun observeUser(uid: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_USERS)
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(User::class.java))
            }
        awaitClose { listener.remove() }
    }

    // ─── Posts ──────────────────────────────────────────

    suspend fun createPost(post: Post) {
        firestore.collection(Constants.COLLECTION_POSTS)
            .document(post.postId)
            .set(post)
            .await()
    }

    suspend fun deletePost(postId: String) {
        firestore.collection(Constants.COLLECTION_POSTS)
            .document(postId)
            .delete()
            .await()
    }

    fun observeFeedPosts(): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_POSTS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.toObjects(Post::class.java) ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    fun observeUserPosts(uid: String): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_POSTS)
            .whereEqualTo("authorUid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.toObjects(Post::class.java) ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    // ─── Likes ──────────────────────────────────────────

    suspend fun likePost(postId: String, uid: String) {
        val batch = firestore.batch()

        // add uid to likes subcollection
        val likeRef = firestore.collection(Constants.COLLECTION_POSTS)
            .document(postId)
            .collection(Constants.COLLECTION_LIKES)
            .document(uid)
        batch.set(likeRef, mapOf("uid" to uid, "createdAt" to System.currentTimeMillis()))

        // increment likeCount on post
        val postRef = firestore.collection(Constants.COLLECTION_POSTS).document(postId)
        batch.update(postRef, "likeCount", FieldValue.increment(1))

        batch.commit().await()
    }

    suspend fun unlikePost(postId: String, uid: String) {
        val batch = firestore.batch()

        val likeRef = firestore.collection(Constants.COLLECTION_POSTS)
            .document(postId)
            .collection(Constants.COLLECTION_LIKES)
            .document(uid)
        batch.delete(likeRef)

        val postRef = firestore.collection(Constants.COLLECTION_POSTS).document(postId)
        batch.update(postRef, "likeCount", FieldValue.increment(-1))

        batch.commit().await()
    }

    suspend fun isPostLikedByUser(postId: String, uid: String): Boolean {
        return firestore.collection(Constants.COLLECTION_POSTS)
            .document(postId)
            .collection(Constants.COLLECTION_LIKES)
            .document(uid)
            .get()
            .await()
            .exists()
    }

    // ─── Comments ───────────────────────────────────────

    suspend fun addComment(comment: Comment) {
        val batch = firestore.batch()

        val commentRef = firestore.collection(Constants.COLLECTION_POSTS)
            .document(comment.postId)
            .collection(Constants.COLLECTION_COMMENTS)
            .document(comment.commentId)
        batch.set(commentRef, comment)

        val postRef = firestore.collection(Constants.COLLECTION_POSTS).document(comment.postId)
        batch.update(postRef, "commentCount", FieldValue.increment(1))

        batch.commit().await()
    }

    fun observeComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_POSTS)
            .document(postId)
            .collection(Constants.COLLECTION_COMMENTS)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val comments = snapshot?.toObjects(Comment::class.java) ?: emptyList()
                trySend(comments)
            }
        awaitClose { listener.remove() }
    }

    // ─── Follow ─────────────────────────────────────────

    suspend fun followUser(currentUid: String, targetUid: String) {
        val batch = firestore.batch()

        // add to following
        val followingRef = firestore.collection(Constants.COLLECTION_USERS)
            .document(currentUid)
            .collection(Constants.COLLECTION_FOLLOWING)
            .document(targetUid)
        batch.set(followingRef, mapOf("uid" to targetUid))

        // add to followers
        val followerRef = firestore.collection(Constants.COLLECTION_USERS)
            .document(targetUid)
            .collection(Constants.COLLECTION_FOLLOWERS)
            .document(currentUid)
        batch.set(followerRef, mapOf("uid" to currentUid))

        // update counts
        batch.update(
            firestore.collection(Constants.COLLECTION_USERS).document(currentUid),
            "followingCount", FieldValue.increment(1)
        )
        batch.update(
            firestore.collection(Constants.COLLECTION_USERS).document(targetUid),
            "followerCount", FieldValue.increment(1)
        )

        batch.commit().await()
    }

    suspend fun unfollowUser(currentUid: String, targetUid: String) {
        val batch = firestore.batch()

        batch.delete(
            firestore.collection(Constants.COLLECTION_USERS)
                .document(currentUid)
                .collection(Constants.COLLECTION_FOLLOWING)
                .document(targetUid)
        )
        batch.delete(
            firestore.collection(Constants.COLLECTION_USERS)
                .document(targetUid)
                .collection(Constants.COLLECTION_FOLLOWERS)
                .document(currentUid)
        )
        batch.update(
            firestore.collection(Constants.COLLECTION_USERS).document(currentUid),
            "followingCount", FieldValue.increment(-1)
        )
        batch.update(
            firestore.collection(Constants.COLLECTION_USERS).document(targetUid),
            "followerCount", FieldValue.increment(-1)
        )

        batch.commit().await()
    }

    suspend fun isFollowingUser(currentUid: String, targetUid: String): Boolean {
        return firestore.collection(Constants.COLLECTION_USERS)
            .document(currentUid)
            .collection(Constants.COLLECTION_FOLLOWING)
            .document(targetUid)
            .get()
            .await()
            .exists()
    }

    suspend fun sendNotification(notification: Notification) {
        firestore.collection(Constants.COLLECTION_NOTIFICATIONS)
            .document(notification.notificationId)
            .set(notification)
            .await()
    }

    fun observeNotifications(uid: String): Flow<List<Notification>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_NOTIFICATIONS)
            .whereEqualTo("toUid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val notifications = snapshot?.toObjects(Notification::class.java) ?: emptyList()
                trySend(notifications)
            }
        awaitClose { listener.remove() }
    }
    suspend fun getFollowingList(currentUid: String): List<String> {
        val snapshot = firestore.collection(Constants.COLLECTION_USERS)
            .document(currentUid)
            .collection(Constants.COLLECTION_FOLLOWING)
            .get()
            .await()
        return snapshot.documents.map { it.id }
    }
}