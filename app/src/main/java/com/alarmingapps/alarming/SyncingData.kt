package com.alarmingapps.alarming

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*


val LOG = "SyncingData"


fun getSyncingData(documentPath: String) : SyncingData {
    return SyncingData(FirebaseFirestore.getInstance().document(documentPath))
}
fun getNewSyncingData(collectionPath: String) : SyncingData {
    //Returns a DocumentReference pointing to a new document with an auto-generated ID within this collection.
    return SyncingData(FirebaseFirestore.getInstance().collection(collectionPath).document())
}

fun getUserSyncingData() : SyncingData {
    val user = FirebaseAuth.getInstance().currentUser
    return SyncingData(FirebaseFirestore.getInstance().collection("users").document(user!!.uid))
}


open class SyncingData(private val documentReference: DocumentReference) {
    /**
     * Each one should be a single document in Cloud Firestore.
     * Most of the time it'll be stored under their UID specifically. but not always, so this general class should ignore that,
     * Written assuming that a document's reference cannot be changed.
     * */

    private lateinit var serverDocument: DocumentSnapshot
    var localData: Map<String, Any> = mapOf()
    private var updateWaiting: Boolean = false
    private var autoUpdateLocal: Boolean = true
    private var unacknowledgedLocalChanges: Boolean = true//meant to track whether server stuff has overwritten local stuff since the last time changes were acknowledged.
    //Having two separate ones helps in the case that, say, a user deletes or changes an object on one client while editing it on another.

    init {
        documentReference.addSnapshotListener{ snapshot, error ->
            //This also acts as a get()
            if (error != null) {
                Log.e(LOG, "Listen failed with error:", error)
            } else {
                serverDocument = snapshot!!
                if (!serverDocument.exists()) {
                    uploadChanges()
                } else {
                    updateWaiting = true
                }
                if (autoUpdateLocal) {
                    updateLocalVersion()
                }
            }
        }
    }

    fun finishedInitialPull() : Boolean {
        return ::serverDocument.isInitialized
    }

    fun uploadChanges() {
        documentReference.set(localData)
    }

    fun overwriteLocal() {
        localData = serverDocument.data!!
        updateWaiting = false
        unacknowledgedLocalChanges = true
    }

    private fun updateLocalVersion() {
        if (updateWaiting) {
            overwriteLocal()
        }
    }

    fun lockAutoUpdatingLocal() {
        autoUpdateLocal = false
    }

    fun unlockAutoUpdatingLocal() {
        autoUpdateLocal = true
        updateLocalVersion()
    }

    fun localHasChanged() : Boolean {
        return unacknowledgedLocalChanges
    }

    fun acknowledgeLocalChanges() {
        unacknowledgedLocalChanges = false
    }

}