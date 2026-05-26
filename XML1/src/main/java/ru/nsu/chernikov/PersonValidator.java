package ru.nsu.chernikov;

import ru.nsu.chernikov.Person;
import java.util.*;

public class PersonValidator {

    public static List<String> validateConsistency(Map<String, Person> persons) {
        List<String> errors = new ArrayList<>();

        for (Person person : persons.values()) {
            // Проверка количества детей
            if (person.getExpectedChildrenCount() > 0 &&
                    !person.isChildrenCountValid()) {
                errors.add(String.format(
                        "Person %s: expected %d children, found %d",
                        person.getId(),
                        person.getExpectedChildrenCount(),
                        person.getChildren().size()
                ));
            }

            // Проверка дубликатов в данных
            checkForDuplicates(person, errors);
        }

        return errors;
    }

    private static void checkForDuplicates(Person person, List<String> errors) {
        Set<String> uniqueChildren = new HashSet<>(person.getChildren());
        if (uniqueChildren.size() != person.getChildren().size()) {
            errors.add(String.format(
                    "Person %s: duplicate children detected",
                    person.getId()
            ));
        }
    }
}