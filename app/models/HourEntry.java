package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import play.data.format.Formats;
import play.data.validation.Constraints.Max;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;
import play.db.jpa.JPA;
import scala.reflect.generic.Trees.This;

@Entity
@SequenceGenerator(name = "hourentry_seq", sequenceName = "hourentry_seq")
public class HourEntry {
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hourentry_seq")
	public Long id;
	
	@ManyToOne
	public ProjectAssignment assignment;
	
	@Required
	@Formats.DateTime(pattern="dd-MM-yy")
	public Date date;
	
	@Required
	@Max(23)
	public int hours;
	
	@Required
	@Max(59)
	public int minutes;
	
	@ManyToMany
	@JoinTable(
			name="hourentry_tag", 
			joinColumns=@JoinColumn(name="hourentry_id"), 
			inverseJoinColumns=@JoinColumn(name="tag_id")
		)
	public List<Tag> tags;
	
	public static List<HourEntry> all() {
		return JPA.em().createQuery("from HourEntry").getResultList();
	}
	
	public static List<HourEntry> allFor(Long userId) {
		return JPA.em()
				.createQuery("from HourEntry he where he.assignment.user.id = :user_id")
				.setParameter("user_id", userId).getResultList();
	}
	
	public static void create(HourEntry entry, String tagsString) {
		
		entry.tags = new ArrayList<Tag>();
		String tags[] = tagsString.split(";");
		for(int i = 0; i < tags.length; i++)			
			entry.tags.add(Tag.create(tags[i]));
		JPA.em().persist(entry);
	}
	
	public static HourEntry read(Long id) {
		return JPA.em().find(HourEntry.class, id);	
	}
	
	public static void update(Long entryId, HourEntry entryToBeUpdated, String tagsString) {
		HourEntry entry = read(entryId);
		entry.assignment = entryToBeUpdated.assignment;
		entry.date = entryToBeUpdated.date;
		entry.hours = entryToBeUpdated.hours;
		entry.minutes = entryToBeUpdated.minutes;
		
		String tags[] = tagsString.split(";");
		entry.tags.clear();
		for(int i = 0; i < tags.length; i++)			
			entry.tags.add(Tag.create(tags[i]));

		JPA.em().merge(entry);
	}
	
	public static void delete(Long id) {
		JPA.em().remove(HourEntry.read(id));
	}
	
	public static boolean hasValidDate(HourEntry hourEntry) {
		return validateDate(hourEntry).isEmpty();		
	}
	
	public static String validateDate(HourEntry hourEntry) {
		return ProjectAssignment.isDateInAssignmentRange(hourEntry.date, hourEntry.assignment.id) ? new String() : "Date not in assignment date range";
	}
	
	public String tagsString() {
		String tagsString = new String();
		for(Tag tag : tags)
			tagsString += tag.tag + ";";
		return tagsString;
	}

}