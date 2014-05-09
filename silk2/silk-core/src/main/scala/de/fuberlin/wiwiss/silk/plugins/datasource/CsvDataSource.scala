package de.fuberlin.wiwiss.silk.plugins.datasource

import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.entity._
import scala.io.Source
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.runtime.resource.Resource
import java.util.logging.Logger

@Plugin(
  id = "csv",
  label = "CSV Source",
  description = "DataSource which retrieves all entities from a csv file.")
case class CsvDataSource(file: Resource, properties: String, separator: Char = ',', prefix: String = "", uri: String = "", regexFilter: String = "") extends DataSource {
  private val logger = Logger.getLogger(classOf[CsvDataSource].getName)

  private val propertyList: Seq[String] = properties.split(separator)

  override def retrievePaths(restriction: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
    for(property <- propertyList) yield {
      (Path.parse("?" + restriction.variable + "/<" + prefix + property + ">"), 1.0)
    }
  }

  override def retrieve(entityDesc: EntityDescription, entities: Seq[String] = Seq.empty): Traversable[Entity] = {

    // Retrieve the indices of the request paths
    val indices =
      for(path <- entityDesc.paths) yield {
        val property = path.operators.head.asInstanceOf[ForwardOperator].property.uri.stripPrefix(prefix)
        val propertyIndex = propertyList.indexOf(property)
        propertyIndex
      }

    // Return new Traversable that generates an entity for each line
    new Traversable[Entity] {
      def foreach[U](f: Entity => U) {
        val inputStream = file.load
        val source = Source.fromInputStream(inputStream)
        try {
          // Iterate through all lines of the source file, filtering lines that
          // match the provided regex expression
          for {
            (line, number) <- source.getLines.zipWithIndex
              .filter(regexFilter.isEmpty || _._1.matches(regexFilter))
          } {

            //Split the line into values
            val allValues = line.split(separator)
            assert(propertyList.size == allValues.size, "Invalid line '" + line + "' with " + allValues.size + " elements. Expected numer of elements " + propertyList.size + ".")
            //Extract requested values
            val values = indices.map(allValues(_))

            val entityURI = if (uri.isEmpty)
              prefix + number
            else
              "\\{([^\\}]+)\\}".r.replaceAllIn(uri, m => {
                val propName = m.group(1)

                assert(propertyList.contains(propName))
                allValues(propertyList.indexOf(propName))
              })


            //Build entity
            f(new Entity(
              uri = entityURI,
              values = values.map(Set(_)),
              desc = entityDesc
            ))
          }
        } finally {
          source.close()
          inputStream.close()
        }
      }
    }
  }
}
