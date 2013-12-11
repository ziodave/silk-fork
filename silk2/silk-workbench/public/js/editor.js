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

var aggregatecounter = 0;
var transformcounter = 0;
var comparecounter = 0;
var sourcecounter = 0;
var elementcounter = 0;
var numberElements = 0;

var transformations = new Object();
var comparators = new Object();
var aggregators = new Object();

var sources = new Array();
var targets = new Array();
var boxes = new Array();

var interlinkId = "";

var sourceDataSet = "";
var targetDataSet = "";
var sourceDataSetVar = "";
var targetDataSetVar = "";
var sourceDataSetRestriction = "";
var targetDataSetRestriction = "";

var customPropertyPathsCreated = false;
var cycleFound = false;

var modificationTimer;
var reverting = false;

var instanceStack = new Array();
var instanceIndex = -1;
var instanceSaved = false;

var confirmOnExit = false;

jsPlumb.Defaults.Container = "droppable";
jsPlumb.Defaults.Connector = new jsPlumb.Connectors.Bezier(80);

var endpointOptions =
{
  endpoint: new jsPlumb.Endpoints.Dot(
  {
    radius: 5
  }),
  isSource: true,
  style: {
    fillStyle: '#890685'
  },
  connectorStyle: {
    gradient: {
      stops: [
        [0, '#890685'],
        [1, '#359ace']
      ]
    },
    strokeStyle: '#890685',
    lineWidth: 5
  },
  isTarget: false,
  anchor: "RightMiddle",
  dropOptions:{ disabled: true }
};

var endpointOptions1 =
{
  endpoint: new jsPlumb.Endpoints.Dot(
  {
    radius: 5
  }),
  isSource: false,
  style: {
    fillStyle: '#359ace'
  },
  connectorStyle: {
    gradient: {
      stops: [
        [0, '#359ace'],
        [1, '#35ceb7']
      ]
    },
    strokeStyle: '#359ace',
    lineWidth: 5
  },
  isTarget: true,
  maxConnections: 100,
  anchor: "LeftMiddle"
};

var endpointOptions2 =
{
  endpoint: new jsPlumb.Endpoints.Dot(
  {
    radius: 5
  }),
  isSource: true,
  style: {
    fillStyle: '#35ceb7'
  },
  connectorStyle: {
    gradient: {
      stops: [
        [0, '#35ceb7'],
        [1, '#359ace']
      ]
    },
    strokeStyle: '#359ace',
    lineWidth: 5
  },
  isTarget: true,
  maxConnections: 1,
  anchor: "RightMiddle",
  dropOptions:{ disabled: true }
};

document.onselectstart = function ()
{
  return false;
};

// Warn the user when he leaves the editor that any unsaved modifications are lost.
window.onbeforeunload = confirmExit;

function confirmExit()
{
  if(confirmOnExit)
  {
    return "The current linkage rule is invalid. Leaving the editor will revert to the last valid linkage rule.";
  }
}

function generateNewElementId() {
    var nameExists;
    do {
        nameExists = false;
        elementcounter = elementcounter + 1;
        $("div.label:contains('unnamed_" + elementcounter + "')").each(function() {
          if (("unnamed_" + elementcounter).length == $(this).text().length) {
              nameExists = true;
          }
        });
    } while (nameExists || $("#unnamed_"+elementcounter).length > 0);
    return "unnamed_"+elementcounter;
}

function getCurrentElementName(elId) {
    var elName = $("#" + elId + " > .label").text();
    if (!elName) elName = $("#" + elId + " > div.label-active > input.label-change").val();
    return elName;
}

function validateLinkSpec() {
    //console.log("validateLinkSpec");
    var errors = new Array();
    var root_elements = new Array();
    var totalNumberElements = 0;
    removeHighlighting();
    if (!reverting) {
        saveInstance();
    }
    reverting = false;

    // if only one element exists
    if ($("#droppable > div.dragDiv").length === 1) {
      var elId = $("#droppable > div.dragDiv").attr('id');
      errorObj = new Object();
      errorObj.id = elId;
      errorObj.message = "Error: Unconnected element '" + getCurrentElementName(elId) + "'.";
      errors.push(errorObj);
    }

    $("#droppable > div.dragDiv").each(function() {
        totalNumberElements++;
        var elId = $(this).attr('id');
        var elName = getCurrentElementName(elId);
        if (elName.search(/[^a-zA-Z0-9_-]+/) !== -1) {
          errorObj = new Object;
          errorObj.id = elId;
          errorObj.message = "Error in element with id '"+ elId +"': An identifier may only contain the following characters (a - z, A - Z, 0 - 9, _, -). The following identifier is not valid: '" + elName + "'.";
          errors.push(errorObj);
        }
        // count root elements
        var target = jsPlumb.getConnections({source: elId});
        if (target[jsPlumb.getDefaultScope()] === undefined || target[jsPlumb.getDefaultScope()].length == 0) {
            root_elements.push(elId);
        }
    });

    if (errors.length == 0) {

      // multiple root elements
      if (root_elements.length > 1) {
        errorObj = new Object();
        var elements = "";
        for (var i = 0; i<root_elements.length; i++) {
          elements += "'" + getCurrentElementName(root_elements[i]) + "'";
          if (i<root_elements.length-1) {
              elements += ", ";
          } else {
              elements += ".";
          }
          highlightElement(root_elements[i], "Error: Multiple root elements found.");
        }
        errorObj.message = "Error: Multiple root elements found: " + elements;
        errorObj.id = 0;
        errors.push(errorObj);

      // no root elements
      } else if (root_elements.length == 0 && totalNumberElements > 0) {
        errorObj = new Object;
        errorObj.id = 0;
        errorObj.message = "Error: No root element found.";
        errors.push(errorObj);

      // cycles
      } else {
        cycleCheck(root_elements[0]);
        if (cycleFound) {
          errorObj = new Object();
          errorObj.id = 0;
          errorObj.message = "Error: A cycle was found in link specification.";
          errors.push(errorObj);
        }
      }

      // forest found
      if ((numberElements > 0) && (totalNumberElements > numberElements)) {
        errorObj = new Object();
        errorObj.id = 0;
        errorObj.message = "Error: Multiple link specifications found.";
        errors.push(errorObj);
      }

      $("#droppable > div.dragDiv").removeAttr("visited");
      cycleFound = false;
      numberElements = 0;
    }

    if (errors.length > 0) {
      // display frontend errors
      updateStatus(errors, null, null);
    } else {
      // send to server
      $.ajax({
        type: 'PUT',
        url: apiUrl + '/linkSpec',
        contentType: 'text/xml',
        processData: false,
        data: serializeLinkSpec(),
        dataType: "json",
        success: function(response) {
          updateStatus(response.error, response.warning, response.info);
        },
        error: function(req) {
          console.log('Error putting linkage rule: ' + req.responseText);
          var response = jQuery.parseJSON(req.responseText);
          updateStatus(response.error, response.warning, response.info);
        }
      });
    }
};

function modifyLinkSpec() {
  //console.log("modifyLinkSpec");
  confirmOnExit = true;
  showPendingIcon();
  clearTimeout(modificationTimer);
  modificationTimer = setTimeout(function() { validateLinkSpec(); }, 2000);
}

function updateStatus(errorMessages, warningMessages, infoMessages) {
  $("#info-box").html("");
  if (errorMessages.length > 0) {
    $("#info-box").append(printErrorMessages(errorMessages));
    showInvalidIcon(errorMessages.length);
  } else if (warningMessages.length > 0) {
    confirmOnExit = false;
    $("#info-box").append(printMessages(warningMessages));
    showWarningIcon(warningMessages.length);
  } else {
    confirmOnExit = false;
    $("#info-box").slideUp(200);
    showValidIcon();
  }
  if (infoMessages != null && infoMessages.length > 0) {
    $("#info > .precision").html(infoMessages[0]).css("display", "inline");
    if (infoMessages[1] !== undefined) {
        $("#info > .recall").html(infoMessages[1]).css("display", "inline");
    } else {
         $("#info > .recall").css("display", "none");
    }
    if (infoMessages[2] !== undefined) {
        $("#info > .measure").html(infoMessages[2]).css("display", "inline");
    } else {
         $("#info > .measure").css("display", "none");
    }
    $("#info").css("display", "block");
  }
}

function showValidIcon() {
    $("#exclamation, #warning, #pending").css("display", "none");
    $("#tick").css("display", "block");
}
function showInvalidIcon(numberMessages) {
    $("#exclamation > .number-messages").html(numberMessages);
    $("#tick, #warning, #pending").css("display", "none");
    $("#exclamation").css("display", "block");
}
function showWarningIcon(numberMessages) {
    $("#warning > .number-messages").html(numberMessages);
    $("#tick, #exclamation, #pending").css("display", "none");
    $("#warning").css("display", "block");
}
function showPendingIcon() {
    $("#exclamation, #warning, #tick").css("display", "none");
    $("#pending").css("display", "block");
}

function printMessages(array) {
  var result = "";
  var c = 1;
  for (var i = 0; i<array.length; i++) {
    result = result + '<div class="msg">' + c + '. ' + encodeHtml(array[i]) + '</div>';
    c++;
  }
  return result;
}

function printErrorMessages(array) {
  var result = "";
  var c = 1;
  for (var i = 0; i<array.length; i++) {
    result = result + '<div class="msg">' + c + '. ' + encodeHtml(array[i].message) + '</div>';
    if (array[i].id) highlightElement(array[i].id, encodeHtml(array[i].message));
    c++;
  }
  return result;
}

function highlightElement(elId, message) {
  var elementToHighlight;
  $("div.label:contains('" + elId + "')").each(function() {
      if (elId.length == $(this).text().length) elementToHighlight = $(this).parent();
  });
  if (!elementToHighlight) elementToHighlight = $("#" + elId);
  elementToHighlight.addClass('highlighted').attr('onmouseover', 'Tip("' + encodeHtml(message) + '")').attr("onmouseout", "UnTip()");
  jsPlumb.repaint(elementToHighlight);
}

function removeHighlighting() {
  $("div .dragDiv").removeClass('highlighted').removeAttr('onmouseover');
  jsPlumb.repaintEverything();
}

function cycleCheck(elId) {
  numberElements++;
  if ($("#"+elId).attr("visited") == "1") {
    cycleFound = true;
  } else {
    $("#"+elId).attr("visited","1");
    var sources = jsPlumb.getConnections({target: elId});
	  if (sources[jsPlumb.getDefaultScope()] !== undefined) {
      for (var i = 0; i < sources[jsPlumb.getDefaultScope()].length; i++) {
        var source = sources[jsPlumb.getDefaultScope()][i].sourceId;
        cycleCheck(source);
      }
    }
  }
}

Array.max = function(array) {
    return Math.max.apply(Math, array);
};

function findLongestPath(xml)
{
  if ($(xml).children().length > 0)
  {
    var xmlHeight = [];
    var i = 0;
    $(xml).children().each(function()
    {
      xmlHeight[i] = findLongestPath($(this));
      i = i+1;
    });
    maxLength = Math.max.apply(null, xmlHeight);
    return 1 + maxLength;
  }
  else
  {
    return 0;
  }
}

function getHelpIcon(description, marginTop) {
  var helpIcon = $(document.createElement('img'));
  helpIcon.attr("src", "../../assets/img/help.png");
  if ((marginTop == null) || (marginTop > 0)) {
    helpIcon.attr("style", "margin-top: 6px; cursor:help;");
  } else {
    helpIcon.attr("style", "margin-bottom: 3px; cursor:help;");
  }
  helpIcon.attr("align", "right");
  helpIcon.attr("title", description);
  return helpIcon;
}

function getDeleteIcon(elementId) {
  var img = $(document.createElement('img'));
  img.attr("src", "../../assets/img/delete.png");
  img.attr("align", "right");
  img.attr("style", "cursor:pointer;");
  //We need to set a time-out here as a element should not remove its own parent in its event handler
  img.attr("onclick", "setTimeout(\"removeElement('" + elementId + "');\", 100);");
  return img;
}

function removeElement(elementId) {
  jsPlumb.removeAllEndpoints(elementId);
  $('#' + elementId).remove();
  UnTip();
  modifyLinkSpec();
}

function parseXML(xml, level, level_y, last_element, max_level, lastElementId)
{
  if (typeof(boxes[level]) === 'undefined') boxes[level] = new Array();

  $(xml).find("> Aggregate").each(function ()
  {
    var boxName = $(this).attr("id");
    var box1 = $(document.createElement('div'));
    box1.addClass('dragDiv aggregateDiv');
    box1.attr("id", boxName);

    box1.prepend('<div class="label">' + boxName + '</div>');

    var height = aggregatecounter * 120 + 20;
    var left = (max_level*250) - ((level + 1) * 250) + 20;
    box1.attr("style", "left: " + left + "px; top: " + height + "px; position: absolute;");

    var number = "#" + boxName;
    jsPlumb.draggable(box1);
    box1.appendTo("#droppable");

    boxes[level].push(box1);

    var box2 = $(document.createElement('small'));
    box2.addClass('name');
    var mytext = document.createTextNode($(this).attr("type"));
    box2.append(mytext);
    box1.append(box2);
    var box2 = $(document.createElement('small'));
    box2.addClass('type');
    var mytext = document.createTextNode("Aggregate");
    box2.append(mytext);
    box1.append(box2);

    var box2 = $(document.createElement('h5'));
    box2.addClass('handler');

    var span = $(document.createElement('div'));
    span.attr("style", "width: 170px; white-space:nowrap; overflow:hidden; float: left;");
    span.attr("title", aggregators[$(this).attr("type")]["name"] + " (Aggregator)");
    var mytext = document.createTextNode(aggregators[$(this).attr("type")]["name"] + " (Aggregator)");
    span.append(mytext);
    box2.append(span);
	
    box2.append(getDeleteIcon(boxName));

    box1.append(box2);

    var box2 = $(document.createElement('div'));
    box2.addClass('content');

    var mytext = document.createTextNode("required: ");
    box2.append(mytext);

    var box3 = $(document.createElement('input'));
    box3.attr("name", "required");
    box3.attr("type", "checkbox");
    if ($(this).attr("required") == "true")
    {
      box3.attr("checked", "checked");
    }
    box2.append(box3);

    var box4 = $(document.createElement('br'));
    box2.append(box4);

    var mytext = document.createTextNode("weight: ");
    box2.append(mytext);

    var box5 = $(document.createElement('input'));
    box5.attr("type", "text");
    box5.attr("name", "weight");
    box5.attr("size", "2");
    box5.attr("value", $(this).attr("weight"));
    box2.append(box5);

    $params = Object();
    $(this).find("> Param").each(function ()
    {
      $params[$(this).attr("name")] = $(this).attr("value");
    });

    $.each(aggregators[$(this).attr("type")]["parameters"], function(j, parameter) {
      var box4 = $(document.createElement('br'));
      box2.append(box4);

      var mytext = document.createTextNode(parameter.name + ": ");
      box2.append(mytext);

      var box5 = $(document.createElement('input'));
      box5.attr("type", "text");
      box5.attr("name", parameter.name);
      box5.attr("size", "10");
      if ($params[parameter.name]) {
        box5.attr("value", $params[parameter.name]);
      }
      box2.append(box5);
    });

    box2.append(getHelpIcon(aggregators[$(this).attr("type")]["description"]));

    box1.append(box2);

    var endp_left = jsPlumb.addEndpoint(boxName, jsPlumb.extend({dropOptions:{ accept: 'canvas[elType="compare"], canvas[elType="aggregate"]', activeClass: 'accepthighlight', hoverClass: 'accepthoverhighlight', over: function(event, ui) { $("body").css('cursor','pointer'); }, out: function(event, ui) { $("body").css('cursor','default'); } }}, endpointOptions1));
    var endp_right = jsPlumb.addEndpoint(boxName, endpointOptions2);
    aggregatecounter = aggregatecounter + 1;
    if (last_element != "")
    {
      jsPlumb.connect(
      {
        sourceEndpoint: endp_right,
        targetEndpoint: last_element
      });
    }
    $(number).nextUntil("div", ".ui-draggable").attr("elType", "aggregate");
    parseXML($(this), level + 1, 0, endp_left, max_level, box1.attr("id"));

  });
  $(xml).find("> Compare").each(function ()
  {
    var boxName = $(this).attr("id");
    var box1 = $(document.createElement('div'));
    box1.addClass('dragDiv compareDiv');
    box1.attr("id", boxName);

    box1.prepend('<div class="label">' + boxName + '</div>');

    var height = 2 * comparecounter * 120 + 20;
    var left = (max_level*250) - ((level + 1) * 250) + 20;
    box1.attr("style", "left: " + left + "px; top: " + height + "px; position: absolute;");

    var number = "#" + boxName;
    jsPlumb.draggable(box1);
    box1.appendTo("#droppable");

    boxes[level].push(box1);

    var box2 = $(document.createElement('small'));
    box2.addClass('name');
    var mytext = document.createTextNode($(this).attr("metric"));
    box2.append(mytext);
    box1.append(box2);
    var box2 = $(document.createElement('small'));
    box2.addClass('type');
    var mytext = document.createTextNode("Compare");
    box2.append(mytext);
    box1.append(box2);

    var box2 = $(document.createElement('h5'));
    box2.addClass('handler');

    var span = $(document.createElement('div'));
    span.attr("style", "width: 170px; white-space:nowrap; overflow:hidden; float: left;");
    span.attr("title", comparators[$(this).attr("metric")]["name"] + " (Comparator)");
    var mytext = document.createTextNode(comparators[$(this).attr("metric")]["name"] + " (Comparator)");
    span.append(mytext);
    box2.append(span);

    box2.append(getDeleteIcon(boxName));

    box1.append(box2);

    var box2 = $(document.createElement('div'));
    box2.addClass('content');

    var mytext = document.createTextNode("required: ");
    box2.append(mytext);

    var box3 = $(document.createElement('input'));
    box3.attr("name", "required");
    box3.attr("type", "checkbox");
    if ($(this).attr("required") == "true")
    {
      box3.attr("checked", "checked");
    }
    box2.append(box3);

    var box4 = $(document.createElement('br'));
    box2.append(box4);

    var mytext = document.createTextNode("threshold: ");
    box2.append(mytext);

    var box5 = $(document.createElement('input'));
    box5.attr("name", "threshold");
    box5.attr("type", "text");
    box5.attr("size", "2");
    box5.attr("value", $(this).attr("threshold"));
    box2.append(box5);

    var box4 = $(document.createElement('br'));
    box2.append(box4);

    var mytext = document.createTextNode("weight: ");
    box2.append(mytext);

    var box5 = $(document.createElement('input'));
    box5.attr("type", "text");
    box5.attr("name", "weight");
    box5.attr("size", "2");
    box5.attr("value", $(this).attr("weight"));
    box2.append(box5);

    $params = Object();
    $(this).find("> Param").each(function ()
    {
        $params[$(this).attr("name")] = $(this).attr("value");
    });

    $.each(comparators[$(this).attr("metric")]["parameters"], function(j, parameter) {
        var box4 = $(document.createElement('br'));
        box2.append(box4);

        var mytext = document.createTextNode(parameter.name + ": ");
        box2.append(mytext);

        var box5 = $(document.createElement('input'));
        box5.attr("type", "text");
        box5.attr("name", parameter.name);
        box5.attr("size", "10");
        if ($params[parameter.name]) {
          box5.attr("value", $params[parameter.name]);
        }
        else if (parameter.optional) {
          box5.attr("value", parameter.defaultValue);
        }
        box2.append(box5);
    });

    box2.append(getHelpIcon(comparators[$(this).attr("metric")]["description"]));

    box1.append(box2);

    var endp_left = jsPlumb.addEndpoint(boxName, jsPlumb.extend({dropOptions:{ accept: 'canvas[elType="transform"], canvas[elType="source"], canvas[elType="target"]', activeClass: 'accepthighlight', hoverClass: 'accepthoverhighlight', over: function(event, ui) { $("body").css('cursor','pointer'); }, out: function(event, ui) { $("body").css('cursor','default'); } }}, endpointOptions1));
    var endp_right = jsPlumb.addEndpoint(boxName, endpointOptions2);
    comparecounter = comparecounter + 1;
    if (last_element != "")
    {
      jsPlumb.connect(
      {
        sourceEndpoint: endp_right,
        targetEndpoint: last_element
      });
    }
    $(number).nextUntil("div", ".ui-draggable").attr("elType", "compare");
    parseXML($(this), level + 1, 0, endp_left, max_level, box1.attr("id"));
  });

  $(xml).find("> TransformInput").each(function ()
  {
    var boxName = $(this).attr("id");
    var box1 = $(document.createElement('div'));
    box1.addClass('dragDiv transformDiv');
    box1.attr("id", boxName);

    box1.prepend('<div class="label">' + boxName + '</div>');

    var height = transformcounter * 120 + 20;
    var left = (max_level*250) - ((level + 1) * 250) + 20;
    box1.attr("style", "left: " + left + "px; top: " + height + "px; position: absolute;");

    var number = "#" + boxName;
    jsPlumb.draggable(box1);
    box1.appendTo("#droppable");

    boxes[level].push(box1);

    var box2 = $(document.createElement('small'));
    box2.addClass('name');
    var mytext = document.createTextNode($(this).attr("function"));
    box2.append(mytext);
    box1.append(box2);
    var box2 = $(document.createElement('small'));
    box2.addClass('type');
    var mytext = document.createTextNode("TransformInput");
    box2.append(mytext);
    box1.append(box2);

    var box2 = $(document.createElement('h5'));
    box2.addClass('handler');

    var span = $(document.createElement('div'));
    span.attr("style", "width: 170px; white-space:nowrap; overflow:hidden; float: left;");
    span.attr("title", transformations[$(this).attr("function")]["name"] + " (Transformation)");
    var mytext = document.createTextNode(transformations[$(this).attr("function")]["name"] + " (Transformation)");
    span.append(mytext);
    box2.append(span);

    box2.append(getDeleteIcon(boxName));

    box1.append(box2);

    var box2 = $(document.createElement('div'));
    box2.addClass('content');

    $params = Object();
    $(this).find("> Param").each(function ()
    {
        $params[$(this).attr("name")] = $(this).attr("value");
    });

    $.each(transformations[$(this).attr("function")]["parameters"], function(j, parameter) {
        if (j > 0) {
            var box4 = $(document.createElement('br'));
        }
        box2.append(box4);

        var mytext = document.createTextNode(parameter.name + ": ");
        box2.append(mytext);

        var box5 = $(document.createElement('input'));
        box5.attr("type", "text");
        box5.attr("name", parameter.name);
        box5.attr("size", "10");
        if ($params[parameter.name]) {
          box5.attr("value", $params[parameter.name]);
        }
        else if (parameter.optional) {
          box5.attr("value", parameter.defaultValue);
        }
        box2.append(box5);
    });

    box2.append(getHelpIcon(transformations[$(this).attr("function")]["description"], transformations[$(this).attr("function")]["parameters"].length));

    box1.append(box2);

    var endp_left = jsPlumb.addEndpoint(boxName, jsPlumb.extend({dropOptions:{ accept: 'canvas[elType="transform"], canvas[elType="source"], canvas[elType="target"]', activeClass: 'accepthighlight', hoverClass: 'accepthoverhighlight', over: function(event, ui) { $("body").css('cursor','pointer'); }, out: function(event, ui) { $("body").css('cursor','default'); } }}, endpointOptions1));
    var endp_right = jsPlumb.addEndpoint(boxName, endpointOptions2);
    transformcounter = transformcounter + 1;
    if (last_element != "")
    {
      jsPlumb.connect(
      {
        sourceEndpoint: endp_right,
        targetEndpoint: last_element
      });
    }
    $(number).nextUntil("div", ".ui-draggable").attr("elType", "transform");
    parseXML($(this), level + 1, 0, endp_left, max_level, box1.attr("id"));
  });
  $(xml).find("> Input").each(function ()
  {
    var pathClass = "sourcePath";
    var path = $(this).attr("path");
    if (path.charAt(1) == targetDataSetVar) pathClass = "targetPath";

    var boxName = $(this).attr("id");
    var box1 = $(document.createElement('div'));
    box1.addClass('dragDiv ' + pathClass);
    box1.attr("id", boxName);

    box1.prepend('<div class="label">' + boxName + '</div>');

    var height = sourcecounter * 120 + 20;
    var left = (max_level*250) - ((level + 1) * 250) + 20;
    box1.attr("style", "left: " + left + "px; top: " + height + "px; position: absolute;");

    var number = "#" + boxName;
    jsPlumb.draggable(box1);
    box1.appendTo("#droppable");

    boxes[level].push(box1);

    var box2 = $(document.createElement('small'));
    box2.addClass('name');
    var mytext = document.createTextNode(encodeHtml($(this).attr("path")));
    box2.append(mytext);
    box1.append(box2);
    var box2 = $(document.createElement('small'));
    box2.addClass('type');
    var mytext = document.createTextNode("Input");
    box2.append(mytext);
    box1.append(box2);
	
    var box2 = $(document.createElement('h5'));
    box2.addClass('handler');

    var span = $(document.createElement('div'));
    span.attr("style", "width: 170px; min-height: 19px; white-space:nowrap; overflow:hidden; float: left;");
    span.attr("title", $(this).attr("path"));
    var mytext = document.createTextNode($(this).attr("path"));
    span.append(mytext);
    box2.append(span);

    box2.append(getDeleteIcon(boxName));

    box1.append(box2);

    var box2 = $(document.createElement('div'));
    box2.addClass('content');

    box1.append(box2);

    var endp_right = jsPlumb.addEndpoint(boxName, endpointOptions);
    sourcecounter = sourcecounter + 1;
    if (last_element != "")
    {
      jsPlumb.connect(
      {
        sourceEndpoint: endp_right,
        targetEndpoint: last_element
      });
    }
    $(number).nextUntil("div", ".ui-draggable").attr("elType", "source");
    parseXML($(this), level + 1, 0, endp_right, max_level, box1.attr("id"));
  });
}

function load(xml)
{
  var linkSpec = $(xml).find('Interlink');

  interlinkId = $(linkSpec).attr("id");

  $(linkSpec).find("> SourceDataset").each(function ()
  {
    sourceDataSet = $(this).attr("dataSource");
    sourceDataSetVar = $(this).attr("var");
    $(this).find("> RestrictTo").each(function ()
    {
      sourceDataSetRestriction = $(this).text();
    });

  });
  $(linkSpec).find("> TargetDataset").each(function ()
  {
    targetDataSet = $(this).attr("dataSource");
    targetDataSetVar = $(this).attr("var");
    $(this).find("> RestrictTo").each(function ()
    {
      targetDataSetRestriction = $(this).text();
    });
  });
  $(linkSpec).find("> LinkageRule").each(function ()
  {
    var max_level = findLongestPath($(this));

    /*
    var userAgent = navigator.userAgent.toLowerCase();
    // Figure out what browser is being used
    jQuery.browser = {
      version: (userAgent.match( /.+(?:rv|it|ra|ie|me)[\/: ]([\d.]+)/ ) || [])[1],
      chrome: /chrome/.test( userAgent ),
      safari: /webkit/.test( userAgent ) && !/chrome/.test( userAgent ),
      opera: /opera/.test( userAgent ),
      msie: /msie/.test( userAgent ) && !/opera/.test( userAgent ),
      mozilla: /mozilla/.test( userAgent ) && !/(compatible|webkit)/.test( userAgent )
    };
    var is_chrome = /chrome/.test( navigator.userAgent.toLowerCase());
    var is_safari = /safari/.test( navigator.userAgent.toLowerCase());
    */

    parseXML($(this), 0, 0, "", max_level, "");
  });
  $(linkSpec).find("> LinkType").each(function ()
  {
    $("#linktype").attr("value", $(this).text());
  });
  $(linkSpec).find("> Filter").each(function ()
  {
    if ($(this).attr("limit") > 0) {
      // $("select[id=linklimit] option[text="+$(this).attr("limit")+"]").attr("selected", true);
      $("#linklimit").val($(this).attr("limit"));
    }
  });
  updateWindowSize();
  rearrangeBoxes();
}


function updateWindowSize() {
  var window_width =  $(window).width();
  var window_height =  $(window).height();
  if (window_width>1100) {
    $(".wrapperEditor").width(window_width-10);
    $("#droppable").width(window_width-280);
  }
  if (window_height > 600) {
    $(".droppable_outer, #droppable").height(window_height-165);
    $(".scrollboxes").height((window_height/5)-63.5);
  }
}

function rearrangeBoxes() {

    for (var i = boxes.length - 1; i >= 0; i--) {
        for (var j = 0; j < boxes[i].length; j++) {

            var box = boxes[i][j];
            var box_id = box.attr("id");
            var child_conns = jsPlumb.getConnections({target: box_id});
            var children = child_conns[jsPlumb.getDefaultScope()];
            if (children.length == 1) {
                var child = children[0].source;
                child.css("top",box.css("top"));
            }
            if (children.length > 1) {
                var first_child = children[0].source;
                var last_child = children[children.length-1].source;
                var top_first = parseInt(first_child.css("top"));
                var bottom_last = parseInt(last_child.css("top")) + parseInt(last_child.height());
                var middle = parseInt((top_first+bottom_last)/2);
                box.css("top",middle-parseInt(box.height()*0.5));
            }
        }
    }
    jsPlumb.repaintEverything();
}

function getHTML(who, deep)
{
  if (!who || !who.tagName) return '';
  var txt, el = document.createElement("div");
  el.appendChild(who.cloneNode(deep));
  txt = el.innerHTML;
  el = null;
  return txt;
}

function createNewElement(xmlDoc, elementId)
{
	var elementIdName = "#"+elementId;
	var elName = ($(elementIdName).children(".name").text());
	var elType = ($(elementIdName).children(".type").text());
	var xml = xmlDoc.createElement(elType);
    var newName = $(elementIdName + " > .label").text();
    if (!newName) newName = $(elementIdName + " > div.label-active > input.label-change").val();
    var newPath = $(elementIdName).children().children("input.new-path").val();
    xml.setAttribute("id", newName);

    if (elType == "Input") {
        var pathInput = $(elementIdName+" > h5 > div > input").val();
        var path = ($(elementIdName+" > h5 > div").text());
        if (pathInput !== undefined) {
          xml.setAttribute("path", pathInput);
        } else {
          xml.setAttribute("path", encodeHtml(path));
        }
	} else if (elType == "TransformInput") {
		xml.setAttribute("function", elName);
    } else if (elType == "Aggregate") {
		xml.setAttribute("type", elName);
	} else if (elType == "Compare") {
		xml.setAttribute("metric", elName);
	}
  var params = $(elementIdName+" > div.content > input");

	var c = jsPlumb.getConnections();
	for (var i = 0; i < c[jsPlumb.getDefaultScope()].length; i++)
    {
        var source = c[jsPlumb.getDefaultScope()][i].sourceId;
        var target = c[jsPlumb.getDefaultScope()][i].targetId;
        if (target == elementId)
        {
          xml.appendChild(createNewElement(xmlDoc, source));
        }
    }

    for (var l = 0; l < params.length; l++) {
      if ($(params[l]).attr("name") == "required") {
        if (($(elementIdName+" > div.content > input[name=required]:checked").val()) == "on") {
            xml.setAttribute("required", "true");
        } else {
            xml.setAttribute("required", "false");
        }
      } else if ($(params[l]).attr("name") == "threshold") {
        xml.setAttribute("threshold", $(params[l]).val());
      } else if ($(params[l]).attr("name") == "weight") {
        xml.setAttribute("weight", $(params[l]).val());
      } else {
        if (elType == "Compare") {
          if ($(params[l]).val() != "") {
            var xml_param = xmlDoc.createElement("Param");
            xml_param.setAttribute("name", $(params[l]).attr("name"));
            xml_param.setAttribute("value", $(params[l]).val());
            xml.appendChild(xml_param);
          }
        } else {
          var xml_param = xmlDoc.createElement("Param");
          xml_param.setAttribute("name", $(params[l]).attr("name"));
          xml_param.setAttribute("value", $(params[l]).val());
          xml.appendChild(xml_param);
        }
      }
    }

    return xml;
}

function serializeLinkSpec() {
  //alert (JSON.stringify(c));
  var c = jsPlumb.getConnections();
  if (c[jsPlumb.getDefaultScope()] !== undefined) {
    var connections = "";
    var sources = [];
    var targets = [];
    for (var i = 0; i < c[jsPlumb.getDefaultScope()].length; i++)
    {
      var source = c[jsPlumb.getDefaultScope()][i].sourceId;
      var target = c[jsPlumb.getDefaultScope()][i].targetId;
      sources[target] = source;
      targets[source] = target;
      connections = connections + source + " -> " + target + ", ";
    }
    //alert (connections);
    var root = null;
    for (var key in sources)
    {
      if (!targets[key])
      {
        root = key;
      }
    }

  }
  //alert(connections + "\n\n" + root);
  var xmlDocument = document.implementation.createDocument('', 'root', null);
  var xml = xmlDocument.createElement("Interlink");
  xml.setAttribute("id", interlinkId);

  var linktype = xmlDocument.createElement("LinkType");
  var linktypeText = xmlDocument.createTextNode($("#linktype").val());
  linktype.appendChild(linktypeText);
  xml.appendChild(linktype);

  var sourceDataset = xmlDocument.createElement("SourceDataset");
  sourceDataset.setAttribute("var", sourceDataSetVar);
  sourceDataset.setAttribute("dataSource", sourceDataSet);
  var restriction = xmlDocument.createElement("RestrictTo");
  var restrictionText = xmlDocument.createTextNode(sourceDataSetRestriction);
  restriction.appendChild(restrictionText);
  sourceDataset.appendChild(restriction);
  xml.appendChild(sourceDataset);

  var targetDataset = xmlDocument.createElement("TargetDataset");
  targetDataset.setAttribute("var", targetDataSetVar);
  targetDataset.setAttribute("dataSource", targetDataSet);
  var restriction = xmlDocument.createElement("RestrictTo");
  var restrictionText = xmlDocument.createTextNode(targetDataSetRestriction);
  restriction.appendChild(restrictionText);
  targetDataset.appendChild(restriction);
  xml.appendChild(targetDataset);

  var LinkageRule = xmlDocument.createElement("LinkageRule");
  if ((root != null) && (connections != ""))
  {
    LinkageRule.appendChild(createNewElement(xmlDocument, root));
  }
  xml.appendChild(LinkageRule);

  var filter = xmlDocument.createElement("Filter");
  if ($("#linklimit :selected").text() != "unlimited")
  {
    filter.setAttribute("limit", $("#linklimit :selected").text());
  }
  xml.appendChild(filter);

  var outputs = xmlDocument.createElement("Outputs");
  xml.appendChild(outputs);

  var xmlString = (new XMLSerializer()).serializeToString(xml);
  xmlString = xmlString.replace(/&amp;/g, "&");
  return xmlString;
}

$(function ()
{
  modifyLinkSpec();

  $("#droppable").droppable(
  //{ tolerance: 'touch' },
  {
    drop: function (ev, ui)
    {
      var draggedId = $(ui.draggable).attr("id");
      var boxid = ui.helper.attr('id');

      if ($("#droppable").find("> #"+ui.helper.attr('id')+"").length == 0) {
        //$(this).append($(ui.helper).clone());
        $.ui.ddmanager.current.cancelHelperRemoval = true;
        ui.helper.appendTo(this);
        /*
        styleString = $("#"+ui.helper.attr('id')).attr("style");
        styleString = styleString.replace("position: absolute", "");
        $("#"+ui.helper.attr('id')).attr("style", styleString);
        */

        $('#' + boxid).prepend('<div class="label">' + boxid + '</div>');
        jsPlumb.draggable($('#' + boxid));

        if (draggedId.search(/aggregator/) != -1)
        {
          jsPlumb.addEndpoint(boxid, jsPlumb.extend({dropOptions:{ accept: 'canvas[elType="compare"], canvas[elType="aggregate"]', activeClass: 'accepthighlight', hoverClass: 'accepthoverhighlight', over: function(event, ui) { $("body").css('cursor','pointer'); }, out: function(event, ui) { $("body").css('cursor','default'); } }}, endpointOptions1));
          jsPlumb.addEndpoint(boxid, endpointOptions2);
          $('#' + boxid).nextUntil("div", ".ui-draggable").attr("elType", "aggregate");
        }
        else if (draggedId.search(/transform/) != -1)
        {
          jsPlumb.addEndpoint(boxid, jsPlumb.extend({dropOptions:{ accept: 'canvas[elType="transform"], canvas[elType="source"], canvas[elType="target"]', activeClass: 'accepthighlight', hoverClass: 'accepthoverhighlight', over: function(event, ui) { $("body").css('cursor','pointer'); }, out: function(event, ui) { $("body").css('cursor','default'); } }}, endpointOptions1));
          jsPlumb.addEndpoint(boxid, endpointOptions2);
          $('#' + boxid).nextUntil("div", ".ui-draggable").attr("elType", "transform");
        }
        else if (draggedId.search(/comparator/) != -1)
        {
          jsPlumb.addEndpoint(boxid, jsPlumb.extend({dropOptions:{ accept: 'canvas[elType="transform"], canvas[elType="source"], canvas[elType="target"]', activeClass: 'accepthighlight', hoverClass: 'accepthoverhighlight', over: function(event, ui) { $("body").css('cursor','pointer'); }, out: function(event, ui) { $("body").css('cursor','default'); } }}, endpointOptions1));
          jsPlumb.addEndpoint(boxid, endpointOptions2);
          $('#' + boxid).nextUntil("div", ".ui-draggable").attr("elType", "compare");
        }
        else if (draggedId.search(/source/) != -1)
        {
          jsPlumb.addEndpoint(boxid, endpointOptions);
          $('#' + boxid).nextUntil("div", ".ui-draggable").attr("elType", "source");
        }
        else if (draggedId.search(/target/) != -1)
        {
          jsPlumb.addEndpoint(boxid, endpointOptions);
          $('#' + boxid).nextUntil("div", ".ui-draggable").attr("elType", "target");
        }
        else
        {
          alert("Invalid Element dropped!");
        }

        // fix the position of the new added box
        var offset = $('#' + boxid).offset();
        var scrollleft = $("#droppable").scrollLeft();
        var scrolltop = $("#droppable").scrollTop();
        var top = offset.top-118+scrolltop+scrolltop;
        var left = offset.left-504+scrollleft+scrollleft;
        $('#' + boxid).attr("style", "left: " + left + "px; top: " + top +  "px; position: absolute;");
        jsPlumb.repaint(boxid);
        modifyLinkSpec();
      }
    }
  });

  $('body').attr('onresize', 'updateWindowSize();');
  $('body').attr('onunload', 'jsPlumb.unload();');

  jsPlumb.bind("jsPlumbConnection", {
    jsPlumbConnection: function(e) {
        modifyLinkSpec();
	}
  });
  jsPlumb.bind("jsPlumbConnectionDetached", {
    jsPlumbConnectionDetached: function(e) {
        modifyLinkSpec();
	}
  });

  $(document).on('change', "input[type!='text']", function(e) {
      modifyLinkSpec();
  });

  $(document).on('keyup', "input[type='text']", function(e) {
      modifyLinkSpec();
  });

  $("#undo").button({ disabled: true });
  $("#redo").button({ disabled: true });

  $("#exclamation, #warning").mouseover(function() {
   $(this).attr("style", "cursor:pointer;");
  });

  $("#exclamation, #warning").click(function() {
    if ($("#info-box").is(':visible')) {
      $("#info-box").slideUp(200);
    } else {
      $("#info-box").css("left", $(window).width()-294+"px");
      $("#info-box").slideDown(200);
    }
  });

  $(document).on('click', ".label", function() {
    var current_label = $(this).html();
    var input = '<input class="label-change" type="text" value="' + current_label + '" />';
    $(this).html(input).addClass('label-active').removeClass('label');
    $(this).children().focus();
  });

  $(document).on('blur', ".label-change", function() {
    var new_label = $(this).val();
    $(this).parent().html(new_label).addClass('label').removeClass('label-active');
  });

  $(document).on('click', "div.sourcePath > h5 > div[class!='active'], div.targetPath > h5 > div[class!='active']", function() {
    var thisPath = $(this).text();
    if (!thisPath) thisPath = $(this).children().val();
    if (thisPath !== undefined) thisPath = encodeHtml(thisPath);
    $(this).addClass('active');
    $(this).html('<input class="new-path" type="text" value="' + thisPath + '" />');
    $(this).parent().css("height", "19px");
    $(this).children().focus();
  });

  $(document).on('blur',".new-path", function() {
    var newPath = $(this).val();
    if (newPath !== undefined) newPath = encodeHtml(newPath);
    $(this).parent().parent().css("height", "15px");
    $(this).parent().parent().parent().children(".name").html(newPath);
    $(this).parent().removeClass('active').attr('title', newPath).html(newPath);
  });

  $(document).on('mouseover', "#source_restriction, #target_restriction", function() {
    var txt = $(this).text();
    Tip(txt, DELAY, 20);
  });
  $(document).on('mouseout', "#source_restriction, #target_restriction", function() {
    UnTip();
  });

});

function undo() {
    if (instanceIndex > 0) loadInstance(instanceIndex - 1);

}
function redo() {
    if (instanceIndex < instanceStack.length - 1) loadInstance(instanceIndex + 1);
}

function loadInstance(index) {
    //console.log("loadInstance("+index+")");
    reverting = true;
    instanceIndex = index;
    updateRevertButtons();
    var elements = instanceStack[index];
    var endpoint_right;
    var endpoint_left;

    jsPlumb.detachEveryConnection();
    $("#droppable > div.dragDiv").each(function () {
        jsPlumb.removeAllEndpoints($(this).attr('id'));
        $(this).remove();
    });

    for (var i = 0; i<elements.length; i++) {

       endpoint_right = null;
       endpoint_left = null;
       var box = elements[i][0].clone();
       var boxid = box.attr('id');
       var boxclass = box.attr('class');

        $("#droppable").append(box);

        if (boxclass.search(/aggregate/) != -1)
        {
          endpoint_left = jsPlumb.addEndpoint(boxid, jsPlumb.extend({dropOptions:{ accept: 'canvas[elType="compare"], canvas[elType="aggregate"]', activeClass: 'accepthighlight', hoverClass: 'accepthoverhighlight', over: function(event, ui) { $("body").css('cursor','pointer'); }, out: function(event, ui) { $("body").css('cursor','default'); } }, uuid: boxid}, endpointOptions1));
          endpoint_right = jsPlumb.addEndpoint(boxid, endpointOptions2);
          box.nextUntil("div", ".ui-draggable").attr("elType", "aggregate");
        }
        if (boxclass.search(/transform/) != -1)
        {
          endpoint_left = jsPlumb.addEndpoint(boxid, jsPlumb.extend({dropOptions:{ accept: 'canvas[elType="transform"], canvas[elType="source"], canvas[elType="target"]', activeClass: 'accepthighlight', hoverClass: 'accepthoverhighlight', over: function(event, ui) { $("body").css('cursor','pointer'); }, out: function(event, ui) { $("body").css('cursor','default'); } }, uuid: boxid}, endpointOptions1));
          endpoint_right = jsPlumb.addEndpoint(boxid, endpointOptions2);
          box.nextUntil("div", ".ui-draggable").attr("elType", "transform");
        }
        if (boxclass.search(/compare/) != -1)
        {
          endpoint_left = jsPlumb.addEndpoint(boxid, jsPlumb.extend({dropOptions:{ accept: 'canvas[elType="transform"], canvas[elType="source"], canvas[elType="target"]', activeClass: 'accepthighlight', hoverClass: 'accepthoverhighlight', over: function(event, ui) { $("body").css('cursor','pointer'); }, out: function(event, ui) { $("body").css('cursor','default'); } }, uuid: boxid}, endpointOptions1));
          endpoint_right = jsPlumb.addEndpoint(boxid, endpointOptions2);
          box.nextUntil("div", ".ui-draggable").attr("elType", "compare");
        }
        if (boxclass.search(/source/) != -1)
        {
          endpoint_right = jsPlumb.addEndpoint(boxid, endpointOptions);
          box.nextUntil("div", ".ui-draggable").attr("elType", "source");
        }
        if (boxclass.search(/target/) != -1)
        {
          endpoint_right = jsPlumb.addEndpoint(boxid, endpointOptions);
          box.nextUntil("div", ".ui-draggable").attr("elType", "target");
        }

        elements[i][2] = endpoint_right;
        jsPlumb.draggable(box);
    }

    for (var j = 0; j<elements.length; j++) {
        var endp_left = elements[j][2];
        var endp_right = jsPlumb.getEndpoint(elements[j][1]);
        if (endp_left && endp_right) {
            jsPlumb.connect({
                sourceEndpoint: endp_left,
                targetEndpoint: endp_right
            });
        }
    }

    modifyLinkSpec();
}

function saveInstance() {
    //console.log("saveInstance");
    var elements = new Array();
    var targets = new Array();
    var i = 0;
    $("#droppable > div.dragDiv").each(function() {
        elements[i] = new Array();
        var box = $(this).clone();
        var id = box.attr('id');
        elements[i][0] = box;
        var conns = jsPlumb.getConnections({source: id});
        targets = conns[jsPlumb.getDefaultScope()];
        if (targets !== undefined && targets.length > 0) {
            var target = targets[0].target;
            elements[i][1] = target.attr('id');
        }
        i++;
    });

    instanceStack[++instanceIndex] = elements;
    instanceStack.splice(instanceIndex + 1);
    updateRevertButtons();
//    if (!instanceSaved) {
//        $("#content").unblock();
//    }
    instanceSaved = true;
}

function updateRevertButtons() {
    if (instanceIndex > 0) {
        $("#undo").button("enable");
    } else {
        $("#undo").button("disable");
    }
    if (instanceIndex  < instanceStack.length - 1) {
        $("#redo").button("enable");
    } else {
        $("#redo").button("disable");
    }
}

function decodeHtml(value)
{
  encodedHtml = value.replace("&lt;", "<");
  encodedHtml = encodedHtml.replace("&gt;", ">");
  return encodedHtml;
}

function encodeHtml(value)
{
  encodedHtml = value.replace("<", "&lt;");
  encodedHtml = encodedHtml.replace(">", "&gt;");
  encodedHtml = encodedHtml.replace("\"", '\\"');
  return encodedHtml;
}

function getPropertyPaths(deleteExisting)
{
  if (deleteExisting)
  {
    $("#paths").empty();
    var box = $(document.createElement('div'));
    box.attr("id", "loading");
    box.attr("style", "width: 230px;");
    var text = document.createTextNode("loading ...");
    box.append(text);
    box.appendTo("#paths");
  }

    if (!customPropertyPathsCreated || deleteExisting) {

        var box = $(document.createElement('div'));
        box.css("padding-top","4px");
        box.html("<span style='font-weight: bold; color: #582271;'>Source:</span> <span id='source_id'></span>").appendTo("#paths");
        box.appendTo("#paths");

        var box = $(document.createElement('div'));
        box.addClass('more');
        box.html("<span class='restriction'><span style='font-weight: bold; float: left; padding-right: 5px; color: #582271;'>Restriction: </span><span id='source_restriction'></span></span><div style='clear: both;'></div>").appendTo("#paths");
        box.appendTo("#paths");

        var box = $(document.createElement('div'));
        box.attr("id", "sourcepaths");
        box.addClass("scrollboxes");
        box.appendTo("#paths");

        var box = $(document.createElement('div'));
        box.addClass('draggable');
        box.attr("id", "source0");
        box.html("<span> </span><small> </small><p>(custom path)</p>");
        box.draggable(
        {
          helper: function ()
          {
            var box1 = $(document.createElement('div'));
            box1.addClass('dragDiv sourcePath');

            var boxid = generateNewElementId();
            box1.attr("id", boxid);

            var box2 = $(document.createElement('small'));
            box2.addClass('name');
            box1.append(box2);

            var box2 = $(document.createElement('small'));
            box2.addClass('type');
            var mytext = document.createTextNode("Input");
            box2.append(mytext);
            box1.append(box2);

            var box2 = $(document.createElement('h5'));
            box2.addClass('handler');
            box2.attr("style", "height: 19px;");

            var span = $(document.createElement('div'));
            span.attr("style", "width: 170px; min-height: 19px; white-space:nowrap; overflow:hidden; float: left;");
            var input = $(document.createElement('input'));
            input.addClass('new_path');
            input.attr("style", "width: 165px;");
            input.attr("type", "text");
            input.val("?" + sourceDataSetVar);
            span.append(input);
            box2.append(span);

            box2.append(getDeleteIcon(boxid));

            box1.append(box2);

            var box2 = $(document.createElement('div'));
            box2.addClass('content');
            box1.append(box2);

            return box1;
          }
        });
        box.appendTo("#sourcepaths");

        var box = $(document.createElement('div'));
        box.css("padding-top","4px");
        box.html("<span style='font-weight: bold; color: #782626;'>Target:</span> <span id='target_id'></span>").appendTo("#paths");
        box.appendTo("#paths");

        var box = $(document.createElement('div'));
        box.addClass('more');
        box.html("<span class='restriction'><span style='font-weight: bold; float: left; padding-right: 5px; color: #782626;'>Restriction:</span><span id='target_restriction'></span></span><div style='clear: both;'></div>").appendTo("#paths");
        box.appendTo("#paths");


        var box = $(document.createElement('div'));
        box.attr("id", "targetpaths");
        box.addClass("scrollboxes");
        box.appendTo("#paths");

        var box = $(document.createElement('div'));
        box.addClass('draggable targets');
        box.attr("id", "target0");
        box.html("<span> </span><small> </small><p>(custom path)</p>");
        box.draggable(
        {
          helper: function ()
          {
            var box1 = $(document.createElement('div'));
            box1.addClass('dragDiv targetPath');
            var boxid = generateNewElementId();
            box1.attr("id", boxid);

            var box2 = $(document.createElement('small'));
            box2.addClass('name');
            box1.append(box2);

            var box2 = $(document.createElement('small'));
            box2.addClass('type');
            var mytext = document.createTextNode("Input");
            box2.append(mytext);
            box1.append(box2);

            var box2 = $(document.createElement('h5'));
            box2.addClass('handler');
            box2.attr("style", "height: 19px;");

            var span = $(document.createElement('div'));
            span.attr("style", "width: 170px; min-height: 19px; white-space:nowrap; overflow:hidden; float: left;");
            var input = $(document.createElement('input'));
            input.attr("style", "width: 165px;");
            input.attr("type", "text");
            input.val("?" + targetDataSetVar);
            span.append(input);
            box2.append(span);

            box2.append(getDeleteIcon(boxid));

            box1.append(box2);

            var box2 = $(document.createElement('div'));
            box2.addClass('content');
            box1.append(box2);

            return box1;
          }
        });
        box.appendTo("#targetpaths");
    }

    $(".restriction").hide();
    customPropertyPathsCreated = true;

  $.getJSON(apiUrl + '/paths', function (data)
  {
    if(data.isLoading)
    {
	  var dot = document.createTextNode(".");
      document.getElementById("loading").appendChild(dot);
      setTimeout("getPropertyPaths();", 1000);
      if ($("#loading").html().length>40) $("#loading").html("loading ...");
    }
    else if (data.error !== undefined)
    {
      alert("Could not load property paths.\nError: " + data.error);
    }
    else
    {
      document.getElementById("paths").removeChild(document.getElementById("loading"));

    $(".restriction").show();
    $("#source_id").html(data.source.id);
    $("#source_restriction").html(data.source.restrictions);

    var list_item_id = 1;

    var sourcepaths = data.source.paths;
    $.each(sourcepaths, function (i, item)
    {
      var box = $(document.createElement('div'));
      box.addClass('draggable');
      box.attr("id", "source" + list_item_id);
      box.attr("title", encodeHtml(item.path));
      box.html("<span></span><p style=\"white-space:nowrap; overflow:hidden;\">" + encodeHtml(item.path) + "</p>");
      box.draggable(
      {
        helper: function ()
        {
          var box1 = $(document.createElement('div'));
          box1.addClass('dragDiv sourcePath');
          var boxid = generateNewElementId();
          box1.attr("id", boxid);

          var box2 = $(document.createElement('small'));
          box2.addClass('name');
          var mytext = document.createTextNode(encodeHtml(item.path));
          box2.append(mytext);
          box1.append(box2);

          var box2 = $(document.createElement('small'));
          box2.addClass('type');
          var mytext = document.createTextNode("Input");
          box2.append(mytext);
          box1.append(box2);

          var box2 = $(document.createElement('h5'));
          box2.addClass('handler');

          var span = $(document.createElement('div'));
          span.attr("style", "width: 170px; min-height: 19px; white-space:nowrap; overflow:hidden; float: left;");
          span.attr("title", item.path);
          var mytext = document.createTextNode(item.path);
          span.append(mytext);
          box2.append(span);

          box2.append(getDeleteIcon(boxid));

          box1.append(box2);

          var box2 = $(document.createElement('div'));
          box2.addClass('content');
          box1.append(box2);

          return box1;
        }
      });
      box.appendTo("#sourcepaths");
      list_item_id = list_item_id + 1;

    });

    /*
    var availablePaths = data.source.availablePaths;
    if (max_paths < availablePaths)
    {
      var box = $(document.createElement('div'));
      box.html("<a href='/linkSpec' class='more'>&darr; more source paths...</a>");
      box.appendTo("#paths");
    }
    */

    $("#target_id").html(data.target.id);
    $("#target_restriction").html(data.target.restrictions);

    var list_item_id = 1;

    var sourcepaths = data.target.paths;
    $.each(sourcepaths, function (i, item)
    {
      var box = $(document.createElement('div'));
      box.addClass('draggable targets');
      box.attr("id", "target" + list_item_id);
      box.attr("title", encodeHtml(item.path));
      box.html("<span></span><small>" + encodeHtml(item.path) + "</small><p style=\"white-space:nowrap; overflow:hidden;\">" + encodeHtml(item.path) + "</p>");
      box.draggable(
      {
        helper: function ()
        {
          var box1 = $(document.createElement('div'));
          box1.addClass('dragDiv targetPath');
          var boxid = generateNewElementId();
          box1.attr("id", boxid);

          var box2 = $(document.createElement('small'));
          box2.addClass('name');
          var mytext = document.createTextNode(encodeHtml(item.path));
          box2.append(mytext);
          box1.append(box2);

          var box2 = $(document.createElement('small'));
          box2.addClass('type');
          var mytext = document.createTextNode("Input");
          box2.append(mytext);
          box1.append(box2);

          var box2 = $(document.createElement('h5'));
          box2.addClass('handler');

          var span = $(document.createElement('div'));
          span.attr("style", "width: 170px; min-height: 19px; white-space:nowrap; overflow:hidden; float: left;");
          span.attr("title", item.path);
          var mytext = document.createTextNode(item.path);
          span.append(mytext);
          box2.append(span);

          box2.append(getDeleteIcon(boxid));

          box1.append(box2);

          var box2 = $(document.createElement('div'));
          box2.addClass('content');
          box1.append(box2);

          return box1;
        }
      });
      box.appendTo("#targetpaths");

      list_item_id = list_item_id + 1;

    });
    /*
    var availablePaths = data.target.availablePaths;
    if (max_paths < availablePaths)
    {
      var box = $(document.createElement('div'));
      box.html('<a href="" class="more">&darr; more target paths... (' + availablePaths + ' in total)</a>');
      box.appendTo("#paths");

    }
    */
    }
    updateWindowSize();
  });
}

function getOperators()
{
  $.ajax(
  {
    type: "GET",
    url: apiUrl + '/operators',
    contentType: "application/json; charset=utf-8",
    dataType: "json",
    timeout: 2000,
    success: function (data, textStatus, XMLHttpRequest)
    {
      // alert("success: " + data + " " + textStatus + " " + XMLHttpRequest.status);
      if (XMLHttpRequest.status >= 200 && XMLHttpRequest.status < 300)
      {
        var list_item_id = 0;

        // Removed old code. HTML will now be generated in the template engine.
        //var box = $(document.createElement('div'));
        //box.attr("style", "color: #0cc481;");
        //box.addClass("boxheaders");
        //box.html("Transformations").appendTo("#operators");
        //box.appendTo("#operators");

        //var box = $(document.createElement('div'));
        //box.attr("id", "transformationbox");
        //box.addClass("scrollboxes");
        //box.appendTo("#operators");

        $.each(data.transformations, function (i, item)
        {
          transformations[item.id] = new Object();
          transformations[item.id]["name"] = item.label;
          transformations[item.id]["description"] = item.description;
          transformations[item.id]["parameters"] = item.parameters;

          //var box = $(document.createElement('div'));
          //box.addClass('draggable tranformations');
          //box.attr("id", "transformation" + list_item_id);
          //box.attr("title", item.description);
          //box.html("<span></span><small>" + item.label + "</small><p>" + item.label + "</p>");

          $("#transformation" + list_item_id).draggable(
          {
            helper: function ()
            {
              var box1 = $(document.createElement('div'));
              box1.addClass('dragDiv transformDiv');
              var boxid = generateNewElementId();
              box1.attr("id", boxid);

              var box2 = $(document.createElement('small'));
              box2.addClass('name');
              var mytext = document.createTextNode(item.id);
              box2.append(mytext);
              box1.append(box2);
              var box2 = $(document.createElement('small'));
              box2.addClass('type');
              var mytext = document.createTextNode("TransformInput");
              box2.append(mytext);
              box1.append(box2);

              var box2 = $(document.createElement('h5'));
              box2.addClass('handler');

              var span = $(document.createElement('div'));
              span.attr("style", "width: 170px; white-space:nowrap; overflow:hidden; float: left;");
              span.attr("title", item.label + " (Transformation)");
              var mytext = document.createTextNode(item.label + " (Transformation)");
              span.append(mytext);
              box2.append(span);

              box2.append(getDeleteIcon(boxid));

              box1.append(box2);

              var box2 = $(document.createElement('div'));
              box2.addClass('content');

              $.each(item.parameters, function (j, parameter)
              {
                if (j > 0)
                {
                  var box4 = $(document.createElement('br'));
                  box2.append(box4);
                }

                var mytext = document.createTextNode(parameter.name + ": ");
                box2.append(mytext);

                var box5 = $(document.createElement('input'));
                box5.attr("name", parameter.name);
                box5.attr("type", "text");
                box5.attr("size", "10");
                if (parameter.optional) {
                  box5.attr("value", parameter.defaultValue);
                }
                box2.append(box5);
              });

              box2.append(getHelpIcon(item.description, item.parameters.length));

              box1.append(box2);
              return box1;
            }
          });
          //box.appendTo("#transformationbox");

          list_item_id = list_item_id + 1;
        });

        var list_item_id = 0;

        // Removed old code. HTML will now be generated in the template engine.
        //var box = $(document.createElement('div'));
        //box.attr("style", "color: #e59829;");
        //box.addClass("boxheaders");
        //box.html("Comparators").appendTo("#operators");
        //box.appendTo("#operators");

        //var box = $('#comparatorbox'));
        //box.attr("id", "comparatorbox");
        //box.addClass("scrollboxes");
        //box.appendTo("#operators");

        $.each(data.comparators, function (i, item)
        {
          comparators[item.id] = new Object();
          comparators[item.id]["name"] = item.label;
          comparators[item.id]["description"] = item.description;
          comparators[item.id]["parameters"] = item.parameters;

          //var box = $(document.createElement('div'));
          //box.addClass('draggable comparators');
          //box.attr("id", "comparator" + list_item_id);
          //box.attr("title", item.description);
          //box.html("<span></span><small>" + item.label + "</small><p>" + item.label + "</p>");

          $("#comparator_" + item.id).draggable(
          {
            helper: function ()
            {
              var box1 = $(document.createElement('div'));
              box1.addClass('dragDiv compareDiv');
              var boxid = generateNewElementId();
              box1.attr("id", boxid);

              var box2 = $(document.createElement('small'));
              box2.addClass('name');
              var mytext = document.createTextNode(item.id);
              box2.append(mytext);
              box1.append(box2);
              var box2 = $(document.createElement('small'));
              box2.addClass('type');
              var mytext = document.createTextNode("Compare");
              box2.append(mytext);
              box1.append(box2);

              var box2 = $(document.createElement('h5'));
              box2.addClass('handler');

              var span = $(document.createElement('div'));
              span.attr("style", "width: 170px; white-space:nowrap; overflow:hidden; float: left;");
              span.attr("title", item.label + " (Comparator)");
              var mytext = document.createTextNode(item.label + " (Comparator)");
              span.append(mytext);
              box2.append(span);

              box2.append(getDeleteIcon(boxid));

              box1.append(box2);

              var box2 = $(document.createElement('div'));
              box2.addClass('content');

              var mytext = document.createTextNode("required: ");
              box2.append(mytext);

              var box3 = $(document.createElement('input'));
              box3.attr("type", "checkbox");
              box3.attr("name", "required");
              box2.append(box3);

              var box4 = $(document.createElement('br'));
              box2.append(box4);

              var mytext = document.createTextNode("threshold: ");
              box2.append(mytext);

              var box5 = $(document.createElement('input'));
              box5.attr("name", "threshold");
              box5.attr("type", "text");
              box5.attr("size", "2");
              box5.attr("value", "0.0");
              box2.append(box5);

              var box4 = $(document.createElement('br'));
              box2.append(box4);

              var mytext = document.createTextNode("weight: ");
              box2.append(mytext);

              var box5 = $(document.createElement('input'));
              box5.attr("name", "weight");
              box5.attr("type", "text");
              box5.attr("size", "2");
              box5.attr("value", "1");
              box2.append(box5);

              $.each(item.parameters, function (j, parameter)
              {
			          var box4 = $(document.createElement('br'));
                box2.append(box4);

                var mytext = document.createTextNode(parameter.name + ": ");
                box2.append(mytext);

                var box5 = $(document.createElement('input'));
                box5.attr("name", parameter.name);
                box5.attr("type", "text");
                box5.attr("size", "10");
                if (parameter.optional) {
                  box5.attr("value", parameter.defaultValue);
                }
                box2.append(box5);
              });

              box2.append(getHelpIcon(item.description));

              box1.append(box2);

              return box1;
            }
          });
          //box.appendTo("#comparatorbox");
          list_item_id = list_item_id + 1;
        });

        var list_item_id = 0;

        var box = $(document.createElement('div'));
        box.attr("style", "color: #1484d4;");
        box.addClass("boxheaders");
        box.html("Aggregators").appendTo("#operators");
        box.appendTo("#operators");

        var box = $(document.createElement('div'));
        box.attr("id", "aggregatorbox");
        box.addClass("scrollboxes");
        box.appendTo("#operators");

        var sourcepaths = data.aggregators;
        $.each(sourcepaths, function (i, item)
        {
          aggregators[item.id] = new Object();
          aggregators[item.id]["name"] = item.label;
          aggregators[item.id]["description"] = item.description;
          aggregators[item.id]["parameters"] = item.parameters;

          var box = $(document.createElement('div'));
          box.addClass('draggable aggregators');
          box.attr("title", item.description);
          box.attr("id", "aggregator" + list_item_id);
          box.html("<span></span><small>" + item.label + "</small><p>" + item.label + "</p>");

          box.draggable(
          {
            helper: function ()
            {
              var box1 = $(document.createElement('div'));
              box1.addClass('dragDiv aggregateDiv');
              var boxid = generateNewElementId();
              box1.attr("id", boxid);

              var box2 = $(document.createElement('small'));
              box2.addClass('name');
              var mytext = document.createTextNode(item.id);
              box2.append(mytext);
              box1.append(box2);
              var box2 = $(document.createElement('small'));
              box2.addClass('type');
              var mytext = document.createTextNode("Aggregate");
              box2.append(mytext);
              box1.append(box2);

              var box2 = $(document.createElement('h5'));
              box2.addClass('handler');

              var span = $(document.createElement('div'));
              span.attr("style", "width: 170px; white-space:nowrap; overflow:hidden; float: left;");
              span.attr("title", item.label + " (Aggregator)");
              var mytext = document.createTextNode(item.label + " (Aggregator)");
              span.append(mytext);
              box2.append(span);

              box2.append(getDeleteIcon(boxid));

              box1.append(box2);

              var box2 = $(document.createElement('div'));
              box2.addClass('content');

              var mytext = document.createTextNode("required: ");
              box2.append(mytext);

              var box3 = $(document.createElement('input'));
              box3.attr("name", "required");
              box3.attr("type", "checkbox");
              box2.append(box3);

              var box4 = $(document.createElement('br'));
              box2.append(box4);

              var mytext = document.createTextNode("weight: ");
              box2.append(mytext);

              var box5 = $(document.createElement('input'));
              box5.attr("type", "text");
              box5.attr("size", "2");
              box5.attr("name", "weight");
              box5.attr("value", "1");
              box2.append(box5);

              $.each(item.parameters, function (j, parameter)
              {
                var box4 = $(document.createElement('br'));
                box2.append(box4);

                var mytext = document.createTextNode(parameter.name + ": ");
                box2.append(mytext);

                var box5 = $(document.createElement('input'));
                box5.attr("name", parameter.name);
                box5.attr("type", "text");
                box5.attr("size", "10");
                if (parameter.optional) {
                  box5.attr("value", parameter.defaultValue);
                }
                box2.append(box5);
              });

              box2.append(getHelpIcon(item.description));

              box1.append(box2);
			  
              return box1;
            }

          });
          box.appendTo("#aggregatorbox");
		  
          list_item_id = list_item_id + 1;
        });

        //Load current link specification from backend
        $.ajax({
          type: "GET",
          url: apiUrl + '/linkSpec',
          dataType: "xml",
          success: load
        });
      }
    },
    error: function (XMLHttpRequest, textStatus, errorThrown)
    {
      alert("Error: " + textStatus + " " + errorThrown);
    }
  });
}

function reloadPropertyPaths() {
    getPropertyPaths(true);
    var answer = confirm("Reloading the cache may take a long time. Do you want to proceed?");
    if (answer) {
        reloadCache();
    }
}

function reloadCache() {
  $.ajax({
    type: "PUT",
    url: '../../api/tasks/' + projectName + '/' + taskName + '/reloadCache',
    dataType: "xml",
    success: load
  });
}