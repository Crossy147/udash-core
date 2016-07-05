package io.udash.bootstrap
package form

import org.scalajs.dom

import scalacss.ScalatagsCss._
import scalatags.JsDom.all._

class UdashInputGroup(groupSize: InputGroupSize)(mds: Modifier*) extends UdashBootstrapComponent {
  lazy val render =
    div(BootstrapStyles.Form.inputGroup, groupSize)(mds).render
}

object UdashInputGroup {
  def apply(groupSize: InputGroupSize = InputGroupSize.Default)(mds: Modifier*): UdashInputGroup =
    new UdashInputGroup(groupSize)(mds)

  def addon(mds: Modifier*): Modifier =
    span(BootstrapStyles.Form.inputGroupAddon)(mds)

  def buttons(mds: Modifier*): Modifier =
    div(BootstrapStyles.Form.inputGroupBtn)(mds)

  def input(el: dom.Element): dom.Element = {
    BootstrapStyles.Form.formControl.classNameIterator
      .foreach(cls => el.classList.add(cls.value))
    el
  }
}