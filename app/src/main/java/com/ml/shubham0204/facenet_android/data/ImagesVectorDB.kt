package com.ml.shubham0204.facenet_android.data

import io.objectbox.kotlin.query
import org.koin.core.annotation.Single

@Single
class ImagesVectorDB {

    private val imagesBox = ObjectBoxStore.store.boxFor(FaceImageRecord::class.java)

    fun addFaceImageRecord(record: FaceImageRecord) {
        imagesBox.put(record)
    }

    fun getNearestEmbeddingPersonName(embedding: FloatArray): FaceImageRecord? {
        return getNearestEmbeddingPersonNames(embedding, 10).firstOrNull()
    }
    fun getNearestEmbeddingPersonNames(embedding: FloatArray, maxResultCount:Int=10): List<FaceImageRecord> {
        /*
        Use maxResultCount to set the maximum number of objects to return by the ANN condition.
        Hint: it can also be used as the "ef" HNSW parameter to increase the search quality in combination
        with a query limit. For example, use maxResultCount of 100 with a Query limit of 10 to have 10 results
        that are of potentially better quality than just passing in 10 for maxResultCount
        (quality/performance tradeoff).
         */
        return imagesBox
            .query(FaceImageRecord_.faceEmbedding.nearestNeighbors(embedding, maxResultCount))
            .build()
            .findWithScores()
            .map { it.get() }
    }

    fun removeFaceRecordsWithPersonID(personID: Long) {
        imagesBox.removeByIds(
            imagesBox.query(FaceImageRecord_.personID.equal(personID)).build().findIds().toList()
        )
    }

    fun getFaceImageRecordsByPersonID(personID: Long): List<FaceImageRecord> {
        return imagesBox.query {
            equal(FaceImageRecord_.personID, personID) // 查詢條件
        }.find() // 執行查詢並回傳結果
    }

}
