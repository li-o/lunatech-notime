package models;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import org.hibernate.annotations.Type;
import org.joda.time.LocalDate;

import play.db.jpa.JPA;
import utils.DateUtil;

@Entity
public class ProjectAssignment implements Comparable<ProjectAssignment> {

	@Id
	@GeneratedValue
	public Long id;

	@ManyToOne
	public Project project;

	@ManyToOne
	public User user;

	@Type(type = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate beginDate;

	@Type(type = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate endDate;

	@Column(scale = 2)
	public BigDecimal hourlyRate = new BigDecimal(0);

	public boolean starred;

	/**
	 * Inserts this project assignment
	 * 
	 * @param projectId
	 *            The id of the related project
	 */
	public void save() {
		JPA.em().persist(this);
	}

	/**
	 * Sets the project and inserts this project assignment
	 * 
	 * @param projectId
	 *            The id of the related project
	 */
	public void saveAndMaximizeTime(Long projectId) {
		this.project = Project.findById(projectId);
		JPA.em().persist(this);
	}

	/**
	 * Sets the project because the form doesn't have a project field and
	 * updates this project assignment
	 * 
	 * @param assignmentId
	 *            The id of the project assignment that is going to be updated
	 * @param projectId
	 *            The id of the related project
	 */
	public void update(Long assignmentId, Long projectId) {
		this.id = assignmentId;
		this.project = Project.findById(projectId);
		JPA.em().merge(this);
	}

	/**
	 * Updates this project assignment
	 * 
	 * @param assignmentId
	 *            The id of the project assignment that is going to be updated
	 * @param projectId
	 *            The id of the related project
	 */
	public void update(Long assignmentId) {
		this.id = assignmentId;
		JPA.em().merge(this);
	}

	/**
	 * Deletes this project assignment
	 */
	public void delete() {
		JPA.em().remove(this);
	}

	/**
	 * Find a project assignment by id
	 * 
	 * @param assignmentId
	 *            The id of the project assignment to be searched for
	 * @return A project assignment
	 */
	public static ProjectAssignment findById(Long assignmentId) {
		return JPA.em().find(ProjectAssignment.class, assignmentId);
	}

	/**
	 * Find all project assignments for a user
	 * 
	 * @param userId
	 *            The id of the user which project assignments are to be
	 *            searched for
	 * @return A List of project assignments
	 */
	public static List<ProjectAssignment> findAllForUser(Long userId) {
		CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
		CriteriaQuery<ProjectAssignment> query = cb
				.createQuery(ProjectAssignment.class);
		Root<ProjectAssignment> assignment = query
				.from(ProjectAssignment.class);

		Join<ProjectAssignment, User> user = assignment
				.join(ProjectAssignment_.user);

		query.where(cb.equal(user.get(User_.id), userId));
		return JPA.em().createQuery(query).getResultList();
	}

	/**
	 * Find all starred project assignments for a user
	 * 
	 * @param userId
	 *            The id of the user which starred project assignments are to be
	 *            searched for
	 * @return A List of project assignments
	 */
	public static List<ProjectAssignment> findAllStarredForUser(Long userId) {
		CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
		CriteriaQuery<ProjectAssignment> query = cb
				.createQuery(ProjectAssignment.class);
		Root<ProjectAssignment> assignment = query
				.from(ProjectAssignment.class);

		Join<ProjectAssignment, User> user = assignment
				.join(ProjectAssignment_.user);

		query.where(cb.equal(user.get(User_.id), userId),
				cb.equal(assignment.get(ProjectAssignment_.starred), true));
		query.orderBy(cb.desc(user.get(User_.id)));
		return JPA.em().createQuery(query).getResultList();
	}

	/**
	 * Find all project assignments for a user on a project
	 * 
	 * @param user
	 *            The user which project assignments are to be searched for
	 * @param project
	 *            The project which project assignments are to be searched for
	 * @return A List of project assignments
	 */
	public static List<ProjectAssignment> findByUserAndProject(User user,
			Project project) {
		CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
		CriteriaQuery<ProjectAssignment> query = cb
				.createQuery(ProjectAssignment.class);
		Root<ProjectAssignment> assignment = query
				.from(ProjectAssignment.class);
		query.where(cb.equal(assignment.get(ProjectAssignment_.user), user),
				cb.equal(assignment.get(ProjectAssignment_.project), project));
		query.orderBy(cb.desc(assignment.get(ProjectAssignment_.id)));
		return JPA.em().createQuery(query).getResultList();
	}

	/**
	 * All assignments for a user
	 * 
	 * @return A Map with as key the assignment id and as value the project name
	 */
	public static Map<String, String> optionsFor(Long userId) {
		LinkedHashMap<String, String> assignments = new LinkedHashMap<String, String>();
		assignments.put("", "");

		for (ProjectAssignment assignment : findAllForUser(userId)) {
			if (ProjectAssignment.isDateInAssignmentRange(new LocalDate(),
					assignment.id))
				assignments.put(assignment.id.toString(),
						assignment.project.name.toString());
		}
		return assignments;
	}

	/**
	 * Assigns all users to a project
	 * 
	 * @param project
	 *            The project all users are assigned to
	 */
	public static void assignAllUsersTo(Project project) {
		List<User> users = User.findAll();
		for (User user : users) {
			if (!isExistingAssignment(user, project)) {
				ProjectAssignment pa = new ProjectAssignment();
				pa.user = user;
				pa.project = project;
				pa.save();
			}
		}
	}

	/**
	 * Assigns all default projects to a user
	 * 
	 * @param user
	 *            The user all default projects are assigned to
	 */
	public static void assignAllDefaultProjectsTo(User user) {
		List<Project> defaultProjects = Project.findAllDefaultProjects();
		for (Project defaultProject : defaultProjects) {
			ProjectAssignment pa = new ProjectAssignment();
			pa.user = user;
			pa.project = defaultProject;
			pa.save();
		}
	}

	/**
	 * Checks if a user is already assigned to a project
	 * 
	 * @param user
	 *            The user to be checked for
	 * @param project
	 *            The project to be checked for
	 * @return A boolean which is true if there are 1 or more assignments with
	 *         this user and project
	 */
	public static boolean isExistingAssignment(User user, Project project) {
		return !findByUserAndProject(user, project).isEmpty();
	}

	// VALIDATION METHODS NEED TO BE REPLACED BY ANNOTATIONS OR BE REWRITTEN
	public static boolean hasValidDates(ProjectAssignment assignment) {
		return validateDates(assignment).isEmpty();
	}

	public static String validateDates(ProjectAssignment assignment) {
		if (assignment.beginDate.compareTo(assignment.endDate) > 0)
			return "Start date is after the End date";
		else
			return new String();
	}

	public static boolean isDateInAssignmentRange(LocalDate date,
			Long assignmentId) {
		ProjectAssignment assignment = ProjectAssignment.findById(assignmentId);
		return DateUtil.between(date, assignment.beginDate,
				assignment.endDate.plusDays(1));
	}

	@Override
	public int compareTo(ProjectAssignment assignment) {
		return this.id.compareTo(assignment.id);
	}

}
