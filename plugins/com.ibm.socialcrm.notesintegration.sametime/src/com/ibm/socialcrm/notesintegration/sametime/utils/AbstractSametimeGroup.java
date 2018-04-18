package com.ibm.socialcrm.notesintegration.sametime.utils;

/****************************************************************
 * IBM Confidential
 *
 * SFA050-Collaboration Source Materials
 *
 * (C) Copyright IBM Corp. 2012
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has been
 * deposited with the U.S. Copyright Office
 *
 ***************************************************************/

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.collaboration.realtime.people.Person;

/**
 * We can't really extend the ST group classes, but we need some of the same capabilities. So we'll build them ourselves.
 * 
 * @author bcbull
 */
public abstract class AbstractSametimeGroup<T extends AbstractSametimeGroup> {
	private List<Member> members;
	private String name;

	public AbstractSametimeGroup(String name) {
		setName(name);
	}

	protected List<Member> internalGetMembers() {
		if (members == null) {
			members = new ArrayList<Member>();
		}
		return members;
	}

	public List<Member> getMembers() {
		return Collections.unmodifiableList(internalGetMembers());
	}

	public boolean addMember(Member member) {
		return internalGetMembers().add(member);
	}

	public boolean removeMember(Member member) {
		return internalGetMembers().remove(member);
	}

	public void clear() {
		internalGetMembers().clear();
	}

	public boolean hasSubGroups() {
		return false;
	}

	public List<T> getSubGroups() {
		return null;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public int getPersonCount() {
		int count = internalGetMembers().size();

		if (getSubGroups() != null) {
			for (AbstractSametimeGroup group : getSubGroups()) {
				count += group.getPersonCount();
			}
		}
		return count;
	}

	public int getOnlinePersonCount() {
		int count = 0;
		for (Member member : internalGetMembers()) {
			int status = member.getSametimePerson().getStatus();
			if (status != Person.STATUS_OFFLINE) {
				count++;
			}
		}

		if (getSubGroups() != null) {
			for (AbstractSametimeGroup group : getSubGroups()) {
				count += group.getOnlinePersonCount();
			}
		}
		return count;
	}

	public Set<Person> recursiveGetSametimePersons() {
		Set<Person> set = new HashSet<Person>();
		for (Member member : internalGetMembers()) {
			set.add(member.getSametimePerson());
		}
		if (getSubGroups() != null) {
			for (AbstractSametimeGroup group : getSubGroups()) {
				set.addAll(group.recursiveGetSametimePersons());
			}
		}
		return set;
	}

}
