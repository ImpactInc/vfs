package com.theorem.ftp.util;

/**
* Compare two elements of a string list.
*/
public class StringCompare implements SortFilter {

	String list[];
	String temp;

	public void set(Object o)
	{
		this.list = (String []) o;
	}

	public void setTemp(int i)
	{
		temp = list[i];
	}

	public void copy(int i, int j)
	{
		list[j] = list[i];
	}

	public void tempSet(int i)
	{
		list[i] = temp;
	}

	public boolean compare(int i)
	{
		return list[i].compareTo(temp) > 0;
	}

}
