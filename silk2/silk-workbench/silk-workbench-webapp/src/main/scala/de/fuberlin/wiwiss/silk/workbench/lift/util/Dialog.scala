/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.workbench.lift.util

import xml.NodeSeq
import net.liftweb.http.SHtml
import net.liftweb.util.Helpers._
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmds.{OnLoad, Script}
import java.util.UUID
import net.liftweb.http.js.JsCmd
import de.fuberlin.wiwiss.silk.workbench.lift.util.JS._

/**
 * Basic dialog with a number of fields.
 */
trait Dialog extends Form {
  /** The title of this dialog */
  def title : String

  /** The parameters of this dialog e.g. ("with" -> "700") */
  protected def dialogParams : List[(String, String)] = ("autoOpen" -> "false") :: ("width" -> "700") :: ("modal" -> "true") :: Nil

  /** The id of this form */
  private lazy val id : String = UUID.randomUUID.toString

  /**
   * Command which opens this dialog.
   */
  def openCmd = Try("Open Dialog") {
    //Update all fields and open the dialog
    updateCmd &
    JsRaw("$('#" + id + "').dialog('open');").cmd
  }

  /**
   * Command which closes this dialog.
   */
  def closeCmd = JsRaw("$('#" + id + "').dialog('close');").cmd

  /**
   * Renders this dialog.
   */
  def render(xhtml : NodeSeq) : NodeSeq = {
    def submit() = Try("Submit Dialog") {
      val cmd = onSubmit()
      closeCmd & cmd
    }

    <div id={id} title={title}> {
      SHtml.ajaxForm(
        super.render ++
          <div style="float:right;">
          { SHtml.ajaxSubmit("OK", submit) }
          </div> ++
        Script(initCmd)
      )
    }
    </div>
  }

  /**
   * Command which initializes this dialog.
   */
  private def initCmd = OnLoad(JsRaw("$('#" + id + "').dialog({ " + dialogParams.map(_.productIterator.mkString(": ")).mkString(", ") + " })").cmd)

  /**
   * Called when the dialog is submitted.
   * Must be overloaded by sub classes in order to read the input values.
   */
  protected def onSubmit(): JsCmd
}