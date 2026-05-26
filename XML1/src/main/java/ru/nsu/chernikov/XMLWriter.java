package ru.nsu.chernikov;

import ru.nsu.chernikov.Person;
import javax.xml.stream.*;
import java.io.*;
import java.util.*;

public class XMLWriter {

    public void writeToXml(Map<String, Person> persons, String outputFile) throws Exception {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = factory.createXMLStreamWriter(
                new FileWriter(outputFile)
        );

        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement("persons");

        for (Person person : persons.values()) {
            writePerson(writer, person);
        }

        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
        writer.close();
    }

    private void writePerson(XMLStreamWriter writer, Person person) throws XMLStreamException {
        writer.writeStartElement("person");
        writer.writeAttribute("id", person.getId());

        writeElement(writer, "firstName", person.getFirstName());
        writeElement(writer, "lastName", person.getLastName());
        writeElement(writer, "gender", person.getGender());
        writeElement(writer, "spouse", person.getSpouse());

        // Parents
        if (!person.getParents().isEmpty()) {
            writer.writeStartElement("parents");
            for (String parent : person.getParents()) {
                writeElement(writer, "parent", parent);
            }
            writer.writeEndElement();
        }

        // Children
        if (!person.getChildren().isEmpty()) {
            writer.writeStartElement("children");
            for (String child : person.getChildren()) {
                writeElement(writer, "child", child);
            }
            writer.writeEndElement();
        }

        // Brothers
        if (!person.getBrothers().isEmpty()) {
            writer.writeStartElement("brothers");
            for (String brother : person.getBrothers()) {
                writeElement(writer, "brother", brother);
            }
            writer.writeEndElement();
        }

        // Sisters
        if (!person.getSisters().isEmpty()) {
            writer.writeStartElement("sisters");
            for (String sister : person.getSisters()) {
                writeElement(writer, "sister", sister);
            }
            writer.writeEndElement();
        }

        writer.writeEndElement(); // person
    }

    private void writeElement(XMLStreamWriter writer, String name, String value)
            throws XMLStreamException {
        if (value != null && !value.isEmpty()) {
            writer.writeStartElement(name);
            writer.writeCharacters(value);
            writer.writeEndElement();
        }
    }
}