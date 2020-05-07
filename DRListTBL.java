
package com.example.DRList;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import java.sql.Timestamp;
import java.time.*;

import com.example.DRList.DRArrayList;
import com.example.DRList.DRIndex;
import com.example.DRList.DRBTree;

public class DRListTBL<T>
{

	DRArrayList<T> dl = new DRArrayList<T>();
	DRArrayList<T> fdl = new DRArrayList<T>();			//1st element

	DRIndex<T> di = new DRIndex<T>();
	DRIndex<T> fdi = new DRIndex<T>();

	DRBTree bt = new DRBTree();
	DRBTree root = new DRBTree();

	int success;
	int deleteNum;
	int size;
	boolean fromGetKey;
}

