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

package org.mbari.vars.vam.controller

import java.net.URI
import java.time.{Duration, Instant}
import java.util.concurrent.TimeUnit

import org.mbari.vars.vam.controllers.{
  MediaController,
  VideoReferenceController,
  VideoSequenceController
}
import org.mbari.vars.vam.dao.jpa.DevelopmentTestDAOFactory
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.Await
import scala.concurrent.duration.{Duration => SDuration}
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.mbari.vars.vam.model.Media

/**
  * @author Brian Schlining
  * @since 2017-03-06T15:54:00
  */
class MediaControllerSpec extends AnyFlatSpec with Matchers with BeforeAndAfterEach {

  private[this] val daoFactory   = DevelopmentTestDAOFactory
  private[this] val controller   = new MediaController(daoFactory)
  private[this] val vsController = new VideoSequenceController(daoFactory)
  private[this] val vrController = new VideoReferenceController(daoFactory)
  private[this] val timeout      = SDuration(2, TimeUnit.SECONDS)

  "MediaController" should "create with minimal arguments" in {

    val fn0 = controller.create(
      getClass.getSimpleName,
      "Ventana",
      "V20160711T012345",
      new URI("http://www.mbari.org/movies/airship.mp4"),
      Instant.parse("2016-07-11T01:23:45Z")
    )

    Await.result(fn0, timeout)

    val fn1 = vsController.findByName(getClass.getSimpleName)
    val vs  = Await.result(fn1, timeout)
    vs shouldBe defined

  }

  it should "create when existing VideoSequence name is found" in {
    val fn0 = controller.create(
      getClass.getSimpleName,
      "Ventana",
      "V20160811T012345",
      new URI("http://www.mbari.org/movies/airship_proxy.mp4"),
      Instant.parse("2016-08-11T01:23:45Z")
    )

    Await.result(fn0, timeout)

    val fn1 = vsController.findByName(getClass.getSimpleName)
    val vs  = Await.result(fn1, timeout)
    vs shouldBe defined
    val v = vs.get
    v.videos.size should be(2)
  }

  it should "create when existing video name is found" in {
    val fn0 = controller.create(
      getClass.getSimpleName,
      "Ventana",
      "V20160811T012345",
      new URI("http://www.mbari.org/movies/airship_mezzanine.mp4"),
      Instant.parse("2016-08-11T01:23:45Z")
    )

    Await.result(fn0, timeout)

    val fn1 = vsController.findByName(getClass.getSimpleName)
    val vs  = Await.result(fn1, timeout)
    vs shouldBe defined
    val v = vs.get
    //println(controller.toJson(v))
    v.videoReferences.size should be(3)
  }

  it should "create with all params" in {
    val fn0 = controller.create(
      getClass.getSimpleName,
      "Ventana",
      "V20160911T012345",
      new URI("http://www.mbari.org/movies/airship_another.mp4"),
      Instant.parse("2016-08-11T01:23:45Z"),
      Some(Duration.ofMinutes(25)),
      Some("video/mp4"),
      Some("h264"),
      Some("aac"),
      Some(1920),
      Some(1080),
      Some(30),
      Some(12345678),
      Some("A test movie"),
      Some(TestUtils.randomSha512())
    )
    Await.result(fn0, timeout)

    val fn1 = vsController.findByName(getClass.getSimpleName)
    val vs  = Await.result(fn1, timeout)
    vs shouldBe defined
    val v = vs.get
    //println(controller.toJson(v))
    v.videoReferences.size should be(4)
  }

  it should "update" in {

    val m0 = Media.build(
      videoSequenceName = Some(getClass.getSimpleName),
      cameraId = Some("A"),
      videoName = Some("A20160911T012345"),
      uri = Some(new URI("http://www.mbari.org/movies/airship_another.mov")),
      startTimestamp = Some(Instant.parse("2016-08-11T01:23:45Z")),
      duration = Some(Duration.ofMinutes(25)),
      container = Some("video/mp4"),
      videoCodec = Some("h264"),
      audioCodec = Some("aac"),
      width = Some(1920),
      height = Some(1080),
      frameRate = Some(30),
      sizeBytes = Some(12345678),
      description = Some("A test movie"),
      sha512 = Some(TestUtils.randomSha512())
    )
    val f0 = controller.createMedia(m0)
    val m1 = Await.result(f0, timeout)
    m1.videoSequenceUuid should not be (null)
    m1.videoReferenceUuid should not be (null)
    m1.videoUuid should not be (null)
    m1.videoSequenceName should be(m0.videoSequenceName)
    m1.startTimestamp should be(m0.startTimestamp)
    m1.duration should be(m0.duration)
    m1.uri should be(m0.uri)
    m1.videoCodec = "prores"
    m1.container = "video/quicktime"
    val f1  = controller.updateMedia(m1)
    val opt = Await.result(f1, timeout)
    opt should not be (None)
    val m2 = opt.get
    m2.videoReferenceUuid should be(m1.videoReferenceUuid)
    m2.videoSequenceUuid should be(m2.videoSequenceUuid)
    m2.videoUuid should be(m2.videoUuid)
    m2.videoSequenceName should be(m1.videoSequenceName)
    m2.startTimestamp should be(m1.startTimestamp)
    m2.duration should be(m1.duration)
    m2.uri should be(m1.uri)
    m2.sha512 should be(m1.sha512)

  }

  it should "findBySha512" in {
    val sha = Array.fill[Byte](64)(48)
    val fn0 = controller.create(
      getClass.getSimpleName,
      "Ventana",
      "V20160922T012345",
      new URI("http://www.mbari.org/movies/V20160922T030405Z.mp4"),
      Instant.parse("2016-09-22T03:04:05Z"),
      Some(Duration.ofMinutes(25)),
      Some("video/mp4"),
      sha512 = Some(sha)
    )
    Await.result(fn0, timeout)

    val fn1 = controller.findBySha512(sha)
    val m   = Await.result(fn1, timeout)
    m shouldBe defined
    m.get.sha512 should be(sha)
  }

  it should "findByVideoSequenceName" in {
    val fn0 = controller.findByVideoSequenceName(getClass.getSimpleName)
    val ms  = Await.result(fn0, timeout)
    ms.size should be(6) // Finds all previous insertions as we used same videoSequenceName
  }

  it should "findByVideoSequenceNameAndTimestamp" in {
    // Exact match of starting date
    val fn1 = controller.findByVideoSequenceNameAndTimestamp(
      getClass.getSimpleName,
      Instant.parse("2016-09-22T03:04:05Z")
    )
    val ms1 = Await.result(fn1, timeout)
    ms1.size should be(1)

    // Date falls within duration
    val fn0 = controller.findByVideoSequenceNameAndTimestamp(
      getClass.getSimpleName,
      Instant.parse("2016-09-22T03:14:05Z")
    )
    val ms = Await.result(fn0, timeout)
    ms.size should be(1)

    // Date is match of end
    val fn2 = controller.findByVideoSequenceNameAndTimestamp(
      getClass.getSimpleName,
      Instant.parse("2016-09-22T03:29:05Z")
    )
    val ms2 = Await.result(fn2, timeout)
    ms2.size should be(1)

    // No match
    val fn3 = controller.findByVideoSequenceNameAndTimestamp(
      getClass.getSimpleName,
      Instant.parse("2016-09-22T04:29:05Z")
    )
    val ms3 = Await.result(fn3, timeout)
    ms3 should be(empty)

  }

}
