package ru.nsu.chernikov;

import ru.nsu.chernikov.Person;
import ru.nsu.chernikov.PersonStaxParser;
import ru.nsu.chernikov.PersonValidator;
import ru.nsu.chernikov.XMLWriter;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            String inputFile = "people.xml";
            String outputFile = "output.xml";

            // Парсинг
            PersonStaxParser parser = new PersonStaxParser();
            Map<String, Person> persons = parser.parse(inputFile);

            // Валидация
            List<String> errors = PersonValidator.validateConsistency(persons);

            if (!errors.isEmpty()) {
                System.out.println("Validation errors:");
                errors.forEach(System.out::println);
            }

            // Запись в XML
            XMLWriter writer = new XMLWriter();
            writer.writeToXml(persons, outputFile);

            System.out.println("Successfully processed " + persons.size() + " persons");
            System.out.println("Output written to: " + outputFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processSiblings(Map<String, Person> persons) {
        // Собираем информацию о siblings из всех persons
        Map<String, Set<String>> siblingRelations = new HashMap<>();

        // Сначала собираем все sibling связи
        for (Person person : persons.values()) {
            // Если в исходных данных есть информация о siblings
            // обрабатываем ее здесь
        }

        // Затем распределяем по brothers/sisters на основе gender
        for (Map.Entry<String, Person> entry : persons.entrySet()) {
            Person person = entry.getValue();

            // Пример логики (нужно адаптировать под вашу структуру данных)
            for (Person other : persons.values()) {
                if (!other.getId().equals(person.getId())) {
                    // Проверяем, являются ли они siblings
                    // (например, по общим родителям)
                    if (haveCommonParents(person, other)) {
                        if ("male".equalsIgnoreCase(other.getGender())) {
                            person.getBrothers().add(other.getId());
                        } else if ("female".equalsIgnoreCase(other.getGender())) {
                            person.getSisters().add(other.getId());
                        }
                    }
                }
            }
        }
    }

    private static boolean haveCommonParents(Person p1, Person p2) {
        // Простая логика проверки общих родителей
        return p1.getParents().stream()
                .anyMatch(parent -> p2.getParents().contains(parent));
    }
}