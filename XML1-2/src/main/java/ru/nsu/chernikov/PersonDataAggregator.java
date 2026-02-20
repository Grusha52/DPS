package ru.nsu.chernikov;

import javax.xml.bind.*;
import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.*;
import java.util.*;

/**
 * Выполняет разбор "грязного" XML-файла с фрагментами записей о людях,
 * агрегирует данные, разрешает текстовые ссылки на имена, проверяет
 * согласованность и выводит «чистый» XML, прошедший проверку по схеме.
 */
public class PersonDataAggregator {

    // ---------- Внутренняя структура для накопления данных ----------
    private static class PersonInfo {
        String id;                      // официальный идентификатор (например, "P123456")
        String firstName;                // имя
        String lastName;                  // фамилия
        String gender;                    // пол ("male"/"female")
        String spouseId;                  // ID супруга(и), если ссылка по ID
        String spouseName;                // имя супруга(и), если задано текстом
        String motherName;                // имя матери
        String fatherName;                // имя отца
        Set<String> parents = new HashSet<>();      // ID родителей
        Set<String> children = new HashSet<>();     // ID детей
        Set<String> siblings = new HashSet<>();     // ID братьев/сестёр
        Integer childrenCountMarker;      // маркер из <children-number value="..."/>
        Integer siblingsCountMarker;      // маркер из <siblings-number value="..."/>
        Set<String> unresolvedChildNames;   // неразрешённые имена детей (будут преобразованы позже)
        Set<String> unresolvedSiblingNames; // неразрешённые имена братьев/сестёр
        Set<String> unresolvedParentNames;  // (зарезервировано)

        // Для объединения по имени (временное поле)
        String canonicalName;             // нормализованное полное имя

        /**
         * Объединить данные другого фрагмента с текущим (для одного человека).
         */
        void merge(PersonInfo other) {
            if (other.firstName != null) firstName = other.firstName;
            if (other.lastName != null) lastName = other.lastName;
            if (other.gender != null) gender = other.gender;
            if (other.spouseId != null) spouseId = other.spouseId;
            if (other.spouseName != null) spouseName = other.spouseName;
            if (other.motherName != null) motherName = other.motherName;
            if (other.fatherName != null) fatherName = other.fatherName;
            parents.addAll(other.parents);
            children.addAll(other.children);
            siblings.addAll(other.siblings);
            if (other.childrenCountMarker != null) childrenCountMarker = other.childrenCountMarker;
            if (other.siblingsCountMarker != null) siblingsCountMarker = other.siblingsCountMarker;
            if (other.unresolvedChildNames != null) {
                if (unresolvedChildNames == null) unresolvedChildNames = new HashSet<>();
                unresolvedChildNames.addAll(other.unresolvedChildNames);
            }
            if (other.unresolvedSiblingNames != null) {
                if (unresolvedSiblingNames == null) unresolvedSiblingNames = new HashSet<>();
                unresolvedSiblingNames.addAll(other.unresolvedSiblingNames);
            }
        }

        /**
         * Вычисляет нормализованное полное имя (обрезает пробелы,
         * заменяет множественные пробелы одним) для сопоставления.
         */
        String getCanonicalName() {
            if (canonicalName != null) return canonicalName;
            String fn = firstName == null ? "" : firstName.trim().replaceAll("\\s+", " ");
            String ln = lastName == null ? "" : lastName.trim().replaceAll("\\s+", " ");
            if (fn.isEmpty() && ln.isEmpty()) return null;
            if (fn.isEmpty()) return ln;
            if (ln.isEmpty()) return fn;
            return fn + " " + ln;
        }
    }

    // ---------- Классы для JAXB (выходной XML) ----------
    @XmlRootElement(name = "persons")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Persons {
        @XmlElement(name = "person")
        private List<Person> persons = new ArrayList<>();
        public List<Person> getPersons() { return persons; }
        public void setPersons(List<Person> persons) { this.persons = persons; }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {"firstName", "lastName", "gender", "spouse", "spouseName",
            "parents", "motherName", "fatherName", "children", "brothers", "sisters"})
    public static class Person {
        @XmlAttribute
        @XmlID                         // помечает поле как ID для ссылок IDREF
        private String id;

        @XmlElement
        private String firstName;

        @XmlElement
        private String lastName;

        @XmlElement
        private String gender;

        @XmlElement
        @XmlIDREF                      // ссылка на другого Person по ID
        private Person spouse;

        @XmlElement
        private String spouseName;      // запасной вариант, если супруг задан именем

        @XmlElementWrapper(name = "parents")
        @XmlElement(name = "parent")
        @XmlIDREF
        private List<Person> parents;

        @XmlElement
        private String motherName;

        @XmlElement
        private String fatherName;

        @XmlElementWrapper(name = "children")
        @XmlElement(name = "child")
        @XmlIDREF
        private List<Person> children;

        @XmlElementWrapper(name = "brothers")
        @XmlElement(name = "brother")
        @XmlIDREF
        private List<Person> brothers;

        @XmlElementWrapper(name = "sisters")
        @XmlElement(name = "sister")
        @XmlIDREF
        private List<Person> sisters;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }

        public Person getSpouse() { return spouse; }
        public void setSpouse(Person spouse) { this.spouse = spouse; }

        public String getSpouseName() { return spouseName; }
        public void setSpouseName(String spouseName) { this.spouseName = spouseName; }

        public List<Person> getParents() { return parents; }
        public void setParents(List<Person> parents) { this.parents = parents; }

        public String getMotherName() { return motherName; }
        public void setMotherName(String motherName) { this.motherName = motherName; }

        public String getFatherName() { return fatherName; }
        public void setFatherName(String fatherName) { this.fatherName = fatherName; }

        public List<Person> getChildren() { return children; }
        public void setChildren(List<Person> children) { this.children = children; }

        public List<Person> getBrothers() { return brothers; }
        public void setBrothers(List<Person> brothers) { this.brothers = brothers; }

        public List<Person> getSisters() { return sisters; }
        public void setSisters(List<Person> sisters) { this.sisters = sisters; }
    }

    // ---------- Главный метод ----------
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Использование: java PersonDataAggregator <входной.xml> <выходной.xml> [схема.xsd]");
            return;
        }
        String inputFile = args[0];
        String outputFile = args[1];
        String schemaFile = args.length > 2 ? args[2] : "person.xsd";

        // 1. Разобрать все фрагменты из входного файла
        List<PersonInfo> fragments = parseInput(inputFile);

        // 2. Объединить фрагменты в записи о людях (по ID или имени)
        Map<String, PersonInfo> personsMap = mergeFragments(fragments);

        // 3. Преобразовать текстовые имена в ID (супруги, родители, дети, сиблинги)
        resolveNameReferences(personsMap);

        // 4. Проверить согласованность маркеров (количество детей, сиблингов)
        validate(personsMap);

        // 5. Преобразовать в объекты JAXB
        Persons root = convertToJAXB(personsMap);

        // 6. Выполнить маршаллинг с проверкой по схеме
        marshalWithValidation(root, outputFile, schemaFile);

        System.out.println("Готово. Результат записан в " + outputFile);
    }

    // ---------- Разбор с помощью StAX ----------
    private static List<PersonInfo> parseInput(String filename) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader reader = factory.createXMLEventReader(new FileInputStream(filename));
        List<PersonInfo> fragments = new ArrayList<>();

        PersonInfo currentPerson = null;
        StringBuilder textBuffer = new StringBuilder();
        String currentElement = null;
        boolean inFullname = false;  // флаг, указывающий, что мы внутри <fullname>

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                StartElement start = event.asStartElement();
                String localName = start.getName().getLocalPart();
                currentElement = localName;
                textBuffer.setLength(0);

                if ("person".equals(localName)) {
                    currentPerson = new PersonInfo();
                    // Попытаться получить id из атрибута
                    String idAttr = getAttributeValue(start, "id");
                    if (idAttr != null) {
                        currentPerson.id = idAttr;
                    }
                    // Также обработать атрибут name как полное имя
                    String nameAttr = getAttributeValue(start, "name");
                    if (nameAttr != null) {
                        parseFullName(nameAttr, currentPerson);
                    }
                } else if (currentPerson != null) {
                    if ("fullname".equals(localName)) {
                        inFullname = true;
                    }
                    // Обработать элементы с атрибутами ref/val/value
                    String ref = getAttributeValue(start, "ref");
                    String val = getAttributeValue(start, "val");
                    String value = getAttributeValue(start, "value");

                    // Множественные ID в атрибуте val (через пробел)
                    if (val != null) {
                        String[] ids = val.split("\\s+");
                        for (String id : ids) {
                            if (!id.isEmpty()) {
                                addReference(currentPerson, localName, id, true);
                            }
                        }
                    } else if (ref != null) {
                        addReference(currentPerson, localName, ref, true);
                    }

                    // Обработать атрибут value для простых полей
                    if (value != null && !"UNKNOWN".equals(value) && !"NONE".equals(value)) {
                        processValueAttribute(currentPerson, localName, value);
                    }

                    // Проверить атрибуты счётчиков (например, <children count="..."/>)
                    String countStr = getAttributeValue(start, "count");
                    if (countStr != null) {
                        try {
                            int count = Integer.parseInt(countStr);
                            if ("children".equals(localName) || "children-number".equals(localName)) {
                                currentPerson.childrenCountMarker = count;
                            } else if ("siblings".equals(localName) || "siblings-number".equals(localName)) {
                                currentPerson.siblingsCountMarker = count;
                            }
                        } catch (NumberFormatException ignored) {}
                    }

                    // Обработать вложенные элементы детей, например <son id="..."/>
                    if ("son".equals(localName) || "daughter".equals(localName) || "child".equals(localName)) {
                        String childId = getAttributeValue(start, "id");
                        if (childId != null) {
                            currentPerson.children.add(childId);
                        }
                        // Если нет атрибута id, захватим текст позже
                    }
                    if ("brother".equals(localName) || "sister".equals(localName)) {
                        String siblingId = getAttributeValue(start, "id");
                        if (siblingId != null) {
                            currentPerson.siblings.add(siblingId);
                        }
                    }
                }
            } else if (event.isEndElement()) {
                EndElement end = event.asEndElement();
                String localName = end.getName().getLocalPart();

                if ("person".equals(localName)) {
                    // Фрагмент закончен – сохраняем
                    if (currentPerson != null) {
                        fragments.add(currentPerson);
                        currentPerson = null;
                    }
                    inFullname = false;
                } else if (currentPerson != null) {
                    String text = textBuffer.toString().trim();
                    if (!text.isEmpty() && !"UNKNOWN".equals(text) && !"NONE".equals(text)) {
                        if (inFullname) {
                            // Внутри <fullname> ожидаем <first> и <family>
                            if ("first".equals(localName)) {
                                currentPerson.firstName = text;
                            } else if ("family".equals(localName)) {
                                currentPerson.lastName = text;
                            }
                        } else {
                            // Обычные элементы с текстовым содержимым
                            switch (localName) {
                                case "firstname":
                                case "firstName":
                                    currentPerson.firstName = text;
                                    break;
                                case "surname":
                                case "lastName":
                                case "family-name":
                                    currentPerson.lastName = text;
                                    break;
                                case "gender":
                                    currentPerson.gender = normalizeGender(text);
                                    break;
                                case "mother":
                                    currentPerson.motherName = text;
                                    break;
                                case "father":
                                    currentPerson.fatherName = text;
                                    break;
                                case "id":
                                    currentPerson.id = text;
                                    break;
                                case "spouce":  // распространённая опечатка
                                case "wife":
                                case "husband":
                                    // Может быть именем или ID
                                    if (isLikelyId(text)) {
                                        currentPerson.spouseId = text;
                                    } else {
                                        currentPerson.spouseName = text;
                                    }
                                    break;
                                case "parent":
                                    if (isLikelyId(text)) {
                                        currentPerson.parents.add(text);
                                    } else {
                                        // Игнорируем текстового родителя (нет общего поля)
                                    }
                                    break;
                                case "child":
                                case "son":
                                case "daughter":
                                    // Текстовое имя ребёнка – сохраняем для последующего разрешения
                                    if (currentPerson.unresolvedChildNames == null)
                                        currentPerson.unresolvedChildNames = new HashSet<>();
                                    currentPerson.unresolvedChildNames.add(text);
                                    break;
                                case "brother":
                                case "sister":
                                    // Текстовое имя брата/сестры – сохраняем для последующего разрешения
                                    if (currentPerson.unresolvedSiblingNames == null)
                                        currentPerson.unresolvedSiblingNames = new HashSet<>();
                                    currentPerson.unresolvedSiblingNames.add(text);
                                    break;
                            }
                        }
                    }
                    if ("fullname".equals(localName)) {
                        inFullname = false;
                    }
                }
                currentElement = null;
            } else if (event.isCharacters()) {
                if (currentElement != null) {
                    textBuffer.append(event.asCharacters().getData());
                }
            }
        }
        reader.close();
        return fragments;
    }

    // Вспомогательные методы для парсинга
    private static String getAttributeValue(StartElement start, String attrName) {
        Attribute attr = start.getAttributeByName(new QName(attrName));
        return attr != null ? attr.getValue() : null;
    }

    private static void parseFullName(String name, PersonInfo person) {
        if (name == null) return;
        String[] parts = name.trim().split("\\s+", 2);
        person.firstName = parts[0];
        if (parts.length > 1) person.lastName = parts[1];
    }

    private static void addReference(PersonInfo person, String relation, String ref, boolean isId) {
        if (!isId) return; // обрабатываем только ID
        switch (relation) {
            case "spouse":
            case "wife":
            case "husband":
            case "spouce":
                person.spouseId = ref;
                break;
            case "parent":
                person.parents.add(ref);
                break;
            case "child":
            case "son":
            case "daughter":
                person.children.add(ref);
                break;
            case "sibling":
            case "siblings":
                person.siblings.add(ref);
                break;
            case "brother":
            case "sister":
                person.siblings.add(ref); // позже разделим по полу
                break;
        }
    }

    private static void processValueAttribute(PersonInfo person, String localName, String value) {
        switch (localName) {
            case "firstname":
            case "firstName":
                person.firstName = value;
                break;
            case "surname":
            case "lastName":
                person.lastName = value;
                break;
            case "gender":
                person.gender = normalizeGender(value);
                break;
            case "id":
                person.id = value;
                break;
            case "wife":
            case "husband":
            case "spouce":
                if (isLikelyId(value)) {
                    person.spouseId = value;
                } else {
                    person.spouseName = value;
                }
                break;
            case "mother":
                person.motherName = value;
                break;
            case "father":
                person.fatherName = value;
                break;
            case "parent":
                if (isLikelyId(value)) {
                    person.parents.add(value);
                }
                break;
            case "children-number":
                try { person.childrenCountMarker = Integer.parseInt(value); } catch (NumberFormatException ignored) {}
                break;
            case "siblings-number":
                try { person.siblingsCountMarker = Integer.parseInt(value); } catch (NumberFormatException ignored) {}
                break;
        }
    }

    private static boolean isLikelyId(String s) {
        return s.matches("P\\d+");
    }

    private static String normalizeGender(String g) {
        if (g == null) return null;
        g = g.trim().toLowerCase();
        if (g.startsWith("f") || "female".equals(g)) return "female";
        if (g.startsWith("m") || "male".equals(g)) return "male";
        return null; // неизвестно
    }

    // ---------- Объединение фрагментов ----------
    private static Map<String, PersonInfo> mergeFragments(List<PersonInfo> fragments) {
        Map<String, PersonInfo> idMap = new HashMap<>();
        Map<String, List<PersonInfo>> nameMap = new HashMap<>();

        // Первый проход: группируем по ID и строим индекс по именам
        for (PersonInfo frag : fragments) {
            if (frag.id != null) {
                // Есть ID – объединяем
                idMap.merge(frag.id, frag, (a, b) -> { a.merge(b); return a; });
            } else {
                // Нет ID, используем каноническое имя
                String name = frag.getCanonicalName();
                if (name != null) {
                    nameMap.computeIfAbsent(name, k -> new ArrayList<>()).add(frag);
                } else {
                    // Нет ни ID, ни имени – создаём синтетический ID
                    String syntheticId = "gen_" + UUID.randomUUID().toString();
                    frag.id = syntheticId;
                    idMap.put(syntheticId, frag);
                }
            }
        }

        // Второй проход: пытаемся сопоставить фрагменты без ID с уже известными по имени
        for (Map.Entry<String, List<PersonInfo>> entry : nameMap.entrySet()) {
            String name = entry.getKey();
            List<PersonInfo> nameless = entry.getValue();
            // Ищем человека с таким именем в idMap
            PersonInfo target = null;
            for (PersonInfo p : idMap.values()) {
                if (name.equals(p.getCanonicalName())) {
                    target = p;
                    break;
                }
            }
            if (target != null) {
                // Нашли – объединяем все безымянные фрагменты с ним
                for (PersonInfo frag : nameless) {
                    target.merge(frag);
                }
            } else {
                // Нет человека с таким именем – создаём нового с синтетическим ID
                PersonInfo combined = new PersonInfo();
                for (PersonInfo frag : nameless) {
                    combined.merge(frag);
                }
                combined.id = "gen_" + UUID.randomUUID().toString();
                // Используем имя как имя/фамилию
                String[] parts = name.split(" ", 2);
                combined.firstName = parts[0];
                if (parts.length > 1) combined.lastName = parts[1];
                idMap.put(combined.id, combined);
            }
        }

        return idMap;
    }

    // ---------- Преобразование текстовых ссылок в ID ----------
    private static void resolveNameReferences(Map<String, PersonInfo> persons) {
        // Строим отображение "каноническое имя" -> ID (предполагаем уникальность имён)
        Map<String, String> nameToId = new HashMap<>();
        for (PersonInfo p : persons.values()) {
            String name = p.getCanonicalName();
            if (name != null) {
                nameToId.put(name, p.id);
            }
        }

        // Для каждого человека разрешаем неразрешённые имена
        for (PersonInfo p : persons.values()) {
            // Пытаемся преобразовать spouseName в spouseId
            if (p.spouseName != null && p.spouseId == null) {
                String id = nameToId.get(p.spouseName);
                if (id != null) {
                    p.spouseId = id;
                    p.spouseName = null; // очищаем имя
                }
            }
            // motherName/fatherName остаются строками (не IDREF)
            // Разрешаем имена детей
            if (p.unresolvedChildNames != null) {
                for (String childName : p.unresolvedChildNames) {
                    String id = nameToId.get(childName);
                    if (id != null) {
                        p.children.add(id);
                    } else {
                        System.err.println("Предупреждение: не удалось разрешить имя ребёнка '" + childName + "' для человека " + p.id);
                    }
                }
                p.unresolvedChildNames = null;
            }
            // Разрешаем имена братьев/сестёр
            if (p.unresolvedSiblingNames != null) {
                for (String sibName : p.unresolvedSiblingNames) {
                    String id = nameToId.get(sibName);
                    if (id != null) {
                        p.siblings.add(id);
                    } else {
                        System.err.println("Предупреждение: не удалось разрешить имя брата/сестры '" + sibName + "' для человека " + p.id);
                    }
                }
                p.unresolvedSiblingNames = null;
            }
        }
    }

    // ---------- Проверка согласованности (маркеры количества) ----------
    private static void validate(Map<String, PersonInfo> persons) {
        for (PersonInfo p : persons.values()) {
            if (p.childrenCountMarker != null) {
                if (p.children.size() != p.childrenCountMarker) {
                    System.err.printf("Предупреждение валидации: у человека %s %d детей, но маркер указывает %d%n",
                            p.id, p.children.size(), p.childrenCountMarker);
                }
            }
            if (p.siblingsCountMarker != null) {
                if (p.siblings.size() != p.siblingsCountMarker) {
                    System.err.printf("Предупреждение валидации: у человека %s %d сиблингов, но маркер указывает %d%n",
                            p.id, p.siblings.size(), p.siblingsCountMarker);
                }
            }
        }
    }

    // ---------- Преобразование в JAXB-объекты ----------
    private static Persons convertToJAXB(Map<String, PersonInfo> personsMap) {
        Persons root = new Persons();
        Map<String, Person> personObjectMap = new HashMap<>();

        // Первый проход: создаём объекты Person без ссылок
        for (PersonInfo info : personsMap.values()) {
            Person p = new Person();
            p.setId(info.id);
            p.setFirstName(info.firstName);
            p.setLastName(info.lastName);
            p.setGender(info.gender);
            p.setSpouseName(info.spouseName);
            p.setMotherName(info.motherName);
            p.setFatherName(info.fatherName);
            // ссылки будут установлены во втором проходе
            root.getPersons().add(p);
            personObjectMap.put(info.id, p);
        }

        // Второй проход: устанавливаем ссылки, используя карту объектов
        for (PersonInfo info : personsMap.values()) {
            Person p = personObjectMap.get(info.id);

            // супруг
            if (info.spouseId != null) {
                p.setSpouse(personObjectMap.get(info.spouseId));
            }

            // родители
            if (!info.parents.isEmpty()) {
                List<Person> parentList = new ArrayList<>();
                for (String pid : info.parents) {
                    Person parent = personObjectMap.get(pid);
                    if (parent != null) parentList.add(parent);
                }
                p.setParents(parentList);
            }

            // дети
            if (!info.children.isEmpty()) {
                List<Person> childList = new ArrayList<>();
                for (String cid : info.children) {
                    Person child = personObjectMap.get(cid);
                    if (child != null) childList.add(child);
                }
                p.setChildren(childList);
            }

            // разделяем сиблингов по полу
            List<Person> brothers = new ArrayList<>();
            List<Person> sisters = new ArrayList<>();
            for (String siblingId : info.siblings) {
                Person sibling = personObjectMap.get(siblingId);
                if (sibling != null && sibling.getGender() != null) {
                    if ("male".equalsIgnoreCase(sibling.getGender())) {
                        brothers.add(sibling);
                    } else if ("female".equalsIgnoreCase(sibling.getGender())) {
                        sisters.add(sibling);
                    } else {
                        System.err.println("Предупреждение: неизвестный пол у сиблинга " + siblingId);
                    }
                } else {
                    System.err.println("Предупреждение: сиблинг " + siblingId + " не найден или не имеет пола");
                }
            }
            p.setBrothers(brothers);
            p.setSisters(sisters);
        }

        return root;
    }

    // ---------- Маршаллинг JAXB с проверкой по схеме ----------
    private static void marshalWithValidation(Persons root, String outputFile, String schemaFile) throws Exception {
        JAXBContext context = JAXBContext.newInstance(Persons.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        // Загружаем схему
        SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = sf.newSchema(new File(schemaFile));
        marshaller.setSchema(schema);

        // Записываем в файл
        marshaller.marshal(root, new File(outputFile));
    }
}