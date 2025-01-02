package com.ml.shubham0204.facenet_android.domain

import com.ml.shubham0204.facenet_android.data.PersonDB
import com.ml.shubham0204.facenet_android.data.PersonRecord
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

@Single
class PersonUseCase(private val personDB: PersonDB) {

    fun addPerson(persion_id: Long, numImages: Long): Long {
        return personDB.addPerson(
            PersonRecord(
                personID = persion_id,
                addTime = System.currentTimeMillis()
            )
        )
    }

    fun removePerson(id: Long) {
        personDB.removePerson(id)
    }

    fun getAll(): Flow<List<PersonRecord>> = personDB.getAll()

    fun getPersonById(personID: Long): PersonRecord? {
        return personDB.getPersonById(personID)
    }

    fun getCount(): Long = personDB.getCount()
}
