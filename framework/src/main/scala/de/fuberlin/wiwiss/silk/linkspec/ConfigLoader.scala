package de.fuberlin.wiwiss.silk.linkspec

import java.io.File
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory
import javax.xml.validation.Validator
import org.xml.sax.SAXException
import de.fuberlin.wiwiss.silk.datasource.DataSource
import xml.{NodeSeq, Node, Elem, XML}

object ConfigLoader
{


    def load(file : File) =
    {
        //validateXML(file)
        val xml = XML.loadFile(file)
        val prefixes = loadPrefixes(xml)
        val dataSources = loadDataSources(xml)
        val linkSpecifications = loadLinkSpecifications(xml, dataSources)

        println(linkSpecifications)
    }

    def validateXML(file : File) : Unit =
    {
        try {
            val factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema")
            val stream = ClassLoader.getSystemClassLoader.getResourceAsStream("de/fuberlin/wiwiss/silk/linkspec/LinkSpecificationLanguage.xsd")
            val schema = factory.newSchema(new StreamSource(stream))
            val validator = schema.newValidator()
            validator.validate(new StreamSource(file))
        } catch {
            case ex : SAXException => throw new ValidationException("Invalid XML. Details: " + ex.getMessage, ex)
        }

    }

    def loadPrefixes(xml : Elem) : Map[String, String] =
    {
        xml \ "Prefixes" \ "Prefix" map(prefix => (prefix \ "@id" text, prefix \ "@namespace" text)) toMap
    }

    def loadDataSources(xml : Elem) : Map[String, DataSource] =
    {
        xml \ "DataSources" \ "DataSource" map(ds => (ds \ "@id" text, DataSource(ds \ "@type" text, loadParams(ds)))) toMap
    }

    def loadParams(element : NodeSeq) : Map[String, String] =
    {
        element \ "Param" map(p => (p \ "@name" text, p \ "@value" text)) toMap
    }

    def loadLinkSpecifications(node : Node, dataSources : Map[String, DataSource]) : Map[String, LinkSpecification] =
    {
        node \ "Interlinks" \ "Interlink" map(p => (p \ "@id" text, loadLinkSpecification(p, dataSources))) toMap
    }

    def loadLinkSpecification(node : Node, dataSources : Map[String, DataSource]) : LinkSpecification =
    {
        new LinkSpecification(
            node \ "LinkType" text,
            loadDatasetSpecification(node \ "SourceDataset", dataSources),
            loadDatasetSpecification(node \ "TargetDataset", dataSources),
            loadAggregation(node \ "LinkCondition" \ "Aggregate" head),
            (node \ "Thresholds" \ "@accept" text).toDouble,
            (node \ "Thresholds" \ "@verify").map(_.text.toDouble).headOption.getOrElse(0.0),
            if (node \ "Limit" isEmpty) null else LinkLimit((node \ "Limit" \ "@max" text).toInt, node \ "Limit" \ "@method" text),
            loadOutputs(node \ "Outputs" \ "Output")
            )
    }

    def loadDatasetSpecification(node : NodeSeq, dataSources : Map[String, DataSource]) : DatasetSpecification =
    {
        val datasourceName = node \ "@dataSource" text
        
        new DatasetSpecification(
            dataSources.get(datasourceName).getOrElse(throw new ValidationException("Datasource " + datasourceName + "not defined.")),
            node \ "@variable" text,
            node \ "RestrictTo" text
            )
    }

    def loadOperators(nodes : Seq[Node]) : Traversable[Operator] =
    {
        nodes.collect
        {
            case node @ <Aggregate>{_}</Aggregate> => loadAggregation(node)
            case node @ <Compare>{_}</Compare> => loadCompare(node)
        }
    }

    def loadAggregation(node : Node) : Aggregation =
    {
        val weightStr = node \ "@weight" text
        val optionalStr = node \ "@optional" text

        Aggregation(
            node \ "@type" text,
            if(weightStr.isEmpty) 1 else weightStr.toInt,
            if(optionalStr.isEmpty) false else optionalStr.toBoolean,
            loadOperators(node.child)
            )
    }

    def loadCompare(node : Node) : Metric =
    {
        val weightStr = node \ "@weight" text
        val optionalStr = node \ "@optional" text

        Metric(
            node \ "@type" text,
            if(weightStr.isEmpty) 1 else weightStr.toInt,
            if(optionalStr.isEmpty) false else optionalStr.toBoolean,
            loadAnyParams(node)
            )
    }

    def loadAnyParams(node : Node) : Map[String, AnyParam] =
    {
        node.map {
            case p @ <Param/> => (p \ "@name" text, new Param(p \ "@value" text))
            case p @ <PathParam/> =>  (p \ "@name" text, new PathParam(p \ "@path" text))
            case p @ <TransformParam>{_}</TransformParam> => (p \ "@name" text, TransformParam(p \ "@function" text, loadAnyParams(p)))
        } toMap
    }

    def loadOutputs(nodes : NodeSeq) : Traversable[Output] =
    {
        nodes.map(loadOutput)
    }

    def loadOutput(node : Node) : Output =
    {
        Output(node \ "@type" text, loadParams(node))
    }
}