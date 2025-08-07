package com.example.ems

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

object FirebaseHelper {
    private val db = FirebaseDatabase.getInstance().reference.child("attendance")
    private val storageRef = FirebaseStorage.getInstance().reference.child("selfies")

    fun uploadAttendance(attendance: AttendanceModel, imageData: ByteArray, onComplete: (Boolean) -> Unit) {
        val key = db.push().key ?: return

        val imageRef = storageRef.child("$key.jpg")
        imageRef.putBytes(imageData).addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                val data = attendance.copy(imageUrl = uri.toString())
                db.child(key).setValue(data).addOnCompleteListener { task ->
                    onComplete(task.isSuccessful)
                }
            }
        }.addOnFailureListener {
            onComplete(false)
        }
    }
}
