package org.mbari.vars.vam.dao.jpa

import java.net.URI
import java.time.{ Duration, Instant }
import java.util.{ Base64, UUID }
import javax.persistence.EntityManager

import org.mbari.vars.vam.dao.VideoReferenceDAO

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

  override def findAll(): Iterable[VideoReference] =
    findByNamedQuery("VideoReference.findAll")

  def findConcurrent(uuid: UUID): Iterable[VideoReference] = {
    findByUUID(uuid) match {
      case None => Nil
      case Some(videoReference) =>
        val startDate = videoReference.video.start
        val endDate = startDate.plus(videoReference.video.duration)
        val siblings = videoReference.video.videoSequence.videoReferences
        siblings.filter(vr => {
          val s = vr.video.start
          val e = s.plus(vr.video.duration)
          s.equals(startDate) ||
            e.equals(endDate) ||
            (s.isAfter(startDate) && s.isBefore(endDate)) ||
            (e.isAfter(startDate) && e.isBefore(endDate)) ||
            (s.isBefore(startDate) && e.isAfter(endDate))
        })
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
