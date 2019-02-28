/*
 * Copyright 2017 Monterey Bay Aquarium Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbari.vars.vam.dao.jpa

import java.net.URI
import java.util.UUID
import javax.persistence.EntityManager
import scala.collection.JavaConverters._

import org.mbari.vars.vam.dao.VideoReferenceDAO

import scala.util.Try

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-05-18T13:11:00
 */
class VideoReferenceDAOImpl(entityManager: EntityManager)
  extends BaseDAO[VideoReference](entityManager)
  with VideoReferenceDAO[VideoReference] {

  override def findByVideoUUID(uuid: UUID): Iterable[VideoReference] =
    findByNamedQuery("VideoReference.findByVideoUUID", Map("uuid" -> uuid))

  override def findByURI(uri: URI): Option[VideoReference] =
    findByNamedQuery("VideoReference.findByURI", Map("uri" -> uri))
      .headOption

  override def findByFileName(filename: String): Iterable[VideoReference] = {
    val query = entityManager.createNamedQuery("VideoReference.findByFileName")
    query.setParameter(1, s"%$filename")
    query.getResultList
      .asScala
      .map(_.toString)
      .map(UUID.fromString)
      .flatMap(findByUUID)
  }

  override def findByUUID(uuid: UUID): Option[VideoReference] =
    findByNamedQuery("VideoReference.findByUuid", Map("uuid" -> uuid))
      .headOption

  override def findAll(): Iterable[VideoReference] =
    findByNamedQuery("VideoReference.findAll")

  def findConcurrent(uuid: UUID): Iterable[VideoReference] = {
    findByUUID(uuid) match {
      case None => Nil
      case Some(videoReference) =>
        val startDate = videoReference.video.start
        val endDate = startDate.plus(videoReference.video.duration)
        val siblings = videoReference.video.videoSequence.videoReferences

        Try(siblings.filter(vr => {
          val s = vr.video.start
          if (s == null) false
          else {
            val e = s.plus(vr.video.duration)
            s.equals(startDate) ||
              e.equals(endDate) ||
              (s.isAfter(startDate) && s.isBefore(endDate)) ||
              (e.isAfter(startDate) && e.isBefore(endDate)) ||
              (s.isBefore(startDate) && e.isAfter(endDate))
          }
        })).getOrElse(Nil)
    }
  }

  override def deleteByUUID(primaryKey: UUID): Unit = {
    val videoReference = findByUUID(primaryKey)
    videoReference.foreach(vr => delete(vr))
  }

  override def findBySha512(sha: Array[Byte]): Option[VideoReference] = {
    //val shaEncoded = Base64.getEncoder.encodeToString(sha)
    findByNamedQuery("VideoReference.findBySha512", Map("sha512" -> sha))
      .headOption
  }
}
