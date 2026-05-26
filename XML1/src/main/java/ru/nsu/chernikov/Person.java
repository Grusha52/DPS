package ru.nsu.chernikov;

import javax.xml.bind.annotation.*;
import java.util.*;

@XmlRootElement(name = "person")
@XmlAccessorType(XmlAccessType.FIELD)
public class Person {
    @XmlAttribute(name = "id")
    private String id;

    @XmlElement(name = "firstName")
    private String firstName;

    @XmlElement(name = "lastName")
    private String lastName;

    @XmlElement(name = "gender")
    private String gender;

    @XmlElement(name = "spouse")
    private String spouse;

    @XmlElementWrapper(name = "parents")
    @XmlElement(name = "parent")
    private List<String> parents = new ArrayList<>();

    @XmlElementWrapper(name = "children")
    @XmlElement(name = "child")
    private List<String> children = new ArrayList<>();

    @XmlElementWrapper(name = "brothers")
    @XmlElement(name = "brother")
    private List<String> brothers = new ArrayList<>();

    @XmlElementWrapper(name = "sisters")
    @XmlElement(name = "sister")
    private List<String> sisters = new ArrayList<>();

    // Для валидации
    private int expectedChildrenCount;

    // Временный список для siblings с неизвестным полом
    @XmlTransient
    private List<String> unknownSiblings = new ArrayList<>();

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getSpouse() { return spouse; }
    public void setSpouse(String spouse) { this.spouse = spouse; }

    public List<String> getParents() { return parents; }
    public void setParents(List<String> parents) { this.parents = parents; }

    public List<String> getChildren() { return children; }
    public void setChildren(List<String> children) { this.children = children; }

    public List<String> getBrothers() { return brothers; }
    public void setBrothers(List<String> brothers) { this.brothers = brothers; }

    public List<String> getSisters() { return sisters; }
    public void setSisters(List<String> sisters) { this.sisters = sisters; }

    public int getExpectedChildrenCount() { return expectedChildrenCount; }
    public void setExpectedChildrenCount(int count) { this.expectedChildrenCount = count; }

    public List<String> getUnknownSiblings() { return unknownSiblings; }
    public void addUnknownSibling(String siblingId) {
        if (!unknownSiblings.contains(siblingId)) {
            unknownSiblings.add(siblingId);
        }
    }

    public boolean isChildrenCountValid() {
        return children.size() == expectedChildrenCount;
    }

    public void addParent(String parentId) {
        if (parentId != null && !parentId.isEmpty() && !parents.contains(parentId)) {
            parents.add(parentId);
        }
    }

    public void addChild(String childId) {
        if (childId != null && !childId.isEmpty() && !children.contains(childId)) {
            children.add(childId);
        }
    }

    public void addSibling(String siblingId, String siblingGender) {
        if (siblingId == null || siblingId.isEmpty()) return;

        if ("male".equalsIgnoreCase(siblingGender)) {
            if (!brothers.contains(siblingId)) {
                brothers.add(siblingId);
            }
        } else if ("female".equalsIgnoreCase(siblingGender)) {
            if (!sisters.contains(siblingId)) {
                sisters.add(siblingId);
            }
        } else {
            addUnknownSibling(siblingId);
        }
    }

    @Override
    public String toString() {
        return String.format("Person{id='%s', name='%s %s', gender='%s'}",
                id, firstName, lastName, gender);
    }
}