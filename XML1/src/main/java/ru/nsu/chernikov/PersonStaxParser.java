package ru.nsu.chernikov;

import javax.xml.namespace.QName;
import ru.nsu.chernikov.Person;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import java.io.*;
import java.util.*;

public class PersonStaxParser {
    private Map<String, Person> persons = new HashMap<>();
    private Map<String, List<String>> siblingRelations = new HashMap<>(); // Временное хранилище

    public Map<String, Person> parse(String xmlFile) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = factory.createXMLEventReader(new FileReader(xmlFile));

        Person currentPerson = null;
        String currentElement = "";
        String currentSiblingId = null;
        String currentSiblingGender = null;

        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();

            switch (event.getEventType()) {
                case XMLEvent.START_ELEMENT:
                    StartElement startElement = event.asStartElement();
                    String qName = startElement.getName().getLocalPart();

                    if ("person".equals(qName)) {
                        currentPerson = new Person();
                        Attribute idAttr = startElement.getAttributeByName(new QName("id"));
                        if (idAttr != null) {
                            String id = idAttr.getValue();
                            currentPerson.setId(id);

                            if (persons.containsKey(id)) {
                                currentPerson = persons.get(id);
                            } else {
                                persons.put(id, currentPerson);
                            }
                        }
                    } else if ("sibling".equals(qName)) {
                        // Извлекаем ID sibling из атрибута, если есть
                        Attribute siblingIdAttr = startElement.getAttributeByName(new QName("id"));
                        Attribute siblingRefAttr = startElement.getAttributeByName(new QName("ref"));
                        if (siblingIdAttr != null) {
                            currentSiblingId = siblingIdAttr.getValue();
                        } else if (siblingRefAttr != null) {
                            currentSiblingId = siblingRefAttr.getValue();
                        }
                        // Если ID в атрибуте не нашли, будем ждать его в тексте
                    } else if (currentPerson != null) {
                        currentElement = qName;
                    }
                    break;

                case XMLEvent.CHARACTERS:
                    Characters characters = event.asCharacters();
                    String data = characters.getData().trim();

                    if (!data.isEmpty() && currentPerson != null && !currentElement.isEmpty()) {
                        processData(currentPerson, currentElement, data, currentSiblingId);
                    }
                    break;

                case XMLEvent.END_ELEMENT:
                    EndElement endElement = event.asEndElement();
                    String endQName = endElement.getName().getLocalPart();

                    if ("person".equals(endQName)) {
                        currentPerson = null;
                    } else if ("sibling".equals(endQName)) {
                        // Сбрасываем временные данные о sibling
                        currentSiblingId = null;
                        currentSiblingGender = null;
                    }
                    currentElement = "";
                    break;
            }
        }

        eventReader.close();

        // Пост-обработка siblings после парсинга всего документа
        processSiblingRelations();

        return persons;
    }

    private void processData(Person person, String element, String data, String siblingIdFromAttr) {
        String elementLower = element.toLowerCase();

        switch (elementLower) {
            case "firstname":
            case "first_name":
            case "name":  // Добавили поддержку тега <name>
                if (person.getFirstName() == null) {
                    person.setFirstName(data);
                }
                break;

            case "lastname":
            case "last_name":
            case "surname":  // Добавили поддержку тега <surname>
                if (person.getLastName() == null) {
                    person.setLastName(data);
                }
                break;

            case "gender":
                if (person.getGender() == null) {
                    person.setGender(data);
                }
                break;

            case "spouse":
                if (person.getSpouse() == null) {
                    person.setSpouse(data);
                }
                break;

            case "father":
                person.addParent(data);
                break;

            case "mother":
                person.addParent(data);
                break;

            case "parent":
                // Обработка тега <parent id="..."> или <parent>ID</parent>
                person.addParent(data);
                break;

            case "child":
            case "children":
                if (data.contains(",")) {
                    // Обработка нескольких children через запятую
                    String[] children = data.split(",");
                    for (String child : children) {
                        String trimmed = child.trim();
                        if (!trimmed.isEmpty()) {
                            person.addChild(trimmed);
                        }
                    }
                } else {
                    person.addChild(data);
                }
                break;

            case "sibling":
                // Обработка sibling - сохраняем временную связь
                String siblingId = siblingIdFromAttr != null ? siblingIdFromAttr : data;
                if (!person.getId().equals(siblingId)) {
                    // Сохраняем двунаправленную связь
                    addSiblingRelation(person.getId(), siblingId);
                }
                break;

            case "number_of_children":
            case "children_count":
            case "childcount":
                try {
                    person.setExpectedChildrenCount(Integer.parseInt(data));
                } catch (NumberFormatException e) {
                    // Логируем или игнорируем
                }
                break;
        }
    }

    private void addSiblingRelation(String personId, String siblingId) {
        // Добавляем связь в обоих направлениях
        siblingRelations.computeIfAbsent(personId, k -> new ArrayList<>()).add(siblingId);
        siblingRelations.computeIfAbsent(siblingId, k -> new ArrayList<>()).add(personId);
    }

    private void processSiblingRelations() {
        // Проходим по всем собранным связям siblings
        for (Map.Entry<String, List<String>> entry : siblingRelations.entrySet()) {
            String personId = entry.getKey();
            List<String> siblingIds = entry.getValue();

            Person person = persons.get(personId);
            if (person == null) continue;

            // Для каждого sibling получаем его пол и добавляем в соответствующий список
            for (String siblingId : siblingIds) {
                Person sibling = persons.get(siblingId);
                if (sibling != null) {
                    String siblingGender = sibling.getGender();
                    if ("male".equalsIgnoreCase(siblingGender)) {
                        if (!person.getBrothers().contains(siblingId)) {
                            person.getBrothers().add(siblingId);
                        }
                    } else if ("female".equalsIgnoreCase(siblingGender)) {
                        if (!person.getSisters().contains(siblingId)) {
                            person.getSisters().add(siblingId);
                        }
                    } else {
                        // Если пол неизвестен, сохраняем в общий список для дальнейшей обработки
                        person.addUnknownSibling(siblingId);
                    }
                }
            }
        }

        // Второй проход для обработки siblings с неизвестным полом
        resolveUnknownSiblings();
    }

    private void resolveUnknownSiblings() {
        // Логика для разрешения siblings с неизвестным полом
        // Например, можно использовать транзитивные связи
        for (Person person : persons.values()) {
            List<String> unknownSiblings = person.getUnknownSiblings();
            if (!unknownSiblings.isEmpty()) {
                for (String unknownSiblingId : unknownSiblings) {
                    Person unknownSibling = persons.get(unknownSiblingId);
                    if (unknownSibling != null) {
                        // Пробуем определить пол через других siblings
                        String gender = inferGenderFromOtherSiblings(unknownSiblingId, person);
                        if (gender != null) {
                            if ("male".equals(gender)) {
                                person.getBrothers().add(unknownSiblingId);
                            } else {
                                person.getSisters().add(unknownSiblingId);
                            }
                        }
                    }
                }
            }
        }
    }

    private String inferGenderFromOtherSiblings(String unknownSiblingId, Person person) {
        // Логика определения пола через транзитивные связи
        Person unknownSibling = persons.get(unknownSiblingId);
        if (unknownSibling == null) return null;

        // Если у unknownSibling есть известные братья/сестры
        for (String brotherId : unknownSibling.getBrothers()) {
            if (brotherId.equals(person.getId())) {
                return "male"; // Если person уже в brothers у unknownSibling
            }
        }

        for (String sisterId : unknownSibling.getSisters()) {
            if (sisterId.equals(person.getId())) {
                return "female"; // Если person уже в sisters у unknownSibling
            }
        }

        return null;
    }
}