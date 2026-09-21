package com.example.biblepaceproject.data

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/** Sign in with Google (Credential Manager) exchanged for a Firebase user. Inert when this build has no google-services.json. */
class AuthManager(private val context: Context) {
    /** The OAuth web client id the google-services plugin generates from google-services.json; absent when Firebase isn't set up. */
    private val webClientId: String? = context.resources
        .getIdentifier("default_web_client_id", "string", context.packageName)
        .takeIf { it != 0 }
        ?.let { context.getString(it) }

    val isConfigured: Boolean = webClientId != null && FirebaseApp.getApps(context).isNotEmpty()

    fun user(): Flow<FirebaseUser?> {
        if (!isConfigured) return flowOf(null)
        return callbackFlow {
            val auth = FirebaseAuth.getInstance()
            val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        }
    }

    /** Throws GetCredentialCancellationException if the person backs out of the Google sheet. */
    suspend fun signIn(activity: Activity) {
        val clientId = webClientId ?: error("Sign-in isn't set up in this build")
        val request = GetCredentialRequest.Builder().addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build()).build()
        val credential = CredentialManager.create(activity).getCredential(activity, request).credential
        check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Unexpected credential type"
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }

    suspend fun signOut() {
        FirebaseAuth.getInstance().signOut()
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    }
}
