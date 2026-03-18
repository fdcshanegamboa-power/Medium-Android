package com.connect.medium.data.repository

import com.connect.medium.data.local.dao.PostDao
import com.connect.medium.data.model.Comment
import com.connect.medium.data.model.Post
import com.connect.medium.data.remote.FirestoreDataSource
import com.connect.medium.utils.Resource
import com.connect.medium.utils.toEntity
import com.connect.medium.utils.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class PostRepository(
    private val firestoreDataSource: FirestoreDataSource,
    private val postDao: PostDao
) {

    fun observeFeedPosts(): Flow<List<Post>> {
        return firestoreDataSource.observeFeedPosts()
            .onEach { posts ->
                // cache to Room
                postDao.insertPosts(posts.map { it.toEntity() })
            }
    }

    fun getCachedPosts(): Flow<List<Post>> {
        return postDao.getAllPosts().map { list -> list.map { it.toModel() } }
    }

    fun observeUserPosts(uid: String): Flow<List<Post>> {
        return firestoreDataSource.observeUserPosts(uid)
    }

    suspend fun createPost(post: Post): Resource<Unit> {
        return try {
            firestoreDataSource.createPost(post)
            postDao.insertPost(post.toEntity())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create post")
        }
    }

    suspend fun deletePost(postId: String): Resource<Unit> {
        return try {
            firestoreDataSource.deletePost(postId)
            postDao.deletePost(postId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete post")
        }
    }

    suspend fun likePost(postId: String, uid: String): Resource<Unit> {
        return try {
            firestoreDataSource.likePost(postId, uid)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to like post")
        }
    }

    suspend fun unlikePost(postId: String, uid: String): Resource<Unit> {
        return try {
            firestoreDataSource.unlikePost(postId, uid)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to unlike post")
        }
    }

    suspend fun isPostLikedByUser(postId: String, uid: String): Boolean {
        return firestoreDataSource.isPostLikedByUser(postId, uid)
    }

    suspend fun addComment(comment: Comment): Resource<Unit> {
        return try {
            firestoreDataSource.addComment(comment)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add comment")
        }
    }

    fun observeComments(postId: String): Flow<List<Comment>> {
        return firestoreDataSource.observeComments(postId)
    }
}