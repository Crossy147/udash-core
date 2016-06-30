package io.udash.bootstrap
package navs

import io.udash._
import org.scalajs.dom

import scalatags.JsDom.all._
import scalatags.JsDom.tags2

class UdashNavbar[ItemType, ElemType <: Property[ItemType]] private
                 (navbarStyle: BootstrapStyles.BootstrapClass)(brand: dom.Element, nav: UdashNav[ItemType, ElemType])
  extends UdashBootstrapComponent {

  import BootstrapTags._

  private val collapseId = UdashBootstrap.newId()
  val navId = UdashBootstrap.newId()

  lazy val render: dom.Element =
    tags2.nav(id := navId.id, BootstrapStyles.Navigation.navbar, navbarStyle)(
      div(BootstrapStyles.containerFluid)(
        div(BootstrapStyles.Navigation.navbarHeader)(
          button(
            tpe := "button", dataToggle := "collapse", dataTarget := s"#$collapseId", aria.expanded := false,
            BootstrapStyles.Navigation.navbarToggle, BootstrapStyles.collapsed
          )(
            span(BootstrapStyles.Visibility.srOnly)("Toggle navigation"),
            span(BootstrapStyles.iconBar), span(BootstrapStyles.iconBar), span(BootstrapStyles.iconBar)
          ),
          brand
        ),

        div(id := collapseId.id, BootstrapStyles.Collapse.collapse, BootstrapStyles.Navigation.navbarCollapse)(nav.render)
      )
    ).render
}

object UdashNavbar {
  def apply[ItemType, ElemType <: Property[ItemType]]
           (brand: dom.Element, nav: UdashNav[ItemType, ElemType]): UdashNavbar[ItemType, ElemType] =
    new UdashNavbar(BootstrapStyles.Navigation.navbarDefault)(brand, nav)

  def inverted[ItemType, ElemType <: Property[ItemType]]
              (brand: dom.Element, nav: UdashNav[ItemType, ElemType]): UdashNavbar[ItemType, ElemType] =
    new UdashNavbar(BootstrapStyles.Navigation.navbarInverse)(brand, nav)
}