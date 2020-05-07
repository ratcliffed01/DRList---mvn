package com.example.DRList;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import java.sql.Timestamp;
import java.time.*;

import com.example.DRList.DRArrayList;
import com.example.DRList.DRCode;
import com.example.DRList.DRListTBL;
import com.example.DRList.DRNoMatchException;
import com.example.DRList.DRFindObjVO;
import com.example.DRList.ProcessTypeVO;
import com.example.DRList.DRFindVO;

import java.lang.reflect.*;
import java.math.*;

public class DRFind<T>
{
	int arrayNum = -1;

    	//===================================================================================
    	public static void debug1(String msg){
		//System.out.println(msg);
    	}
    	//===================================================================================
    	public static void debug2(String msg){
		System.out.println(msg);
    	}
	//====================================================
	public int getOpCnt(String operator) throws DRNoMatchException {

		int opCnt = 99;
		String[] opArray = {"<","=",">","LIKE","MIN","MAX"};
		String op = operator.toUpperCase();

		for (int i = 0; i < opArray.length; i++)
			if (op.equals(opArray[i])) opCnt = i;

		if (opCnt == 99) throw new DRNoMatchException("Invalid operator sign");

		return opCnt;
		
	}
	//================================================
	public DRFindVO checkForMethod(String fieldName, DRFindVO vo, Class c1) 
	{
		try{
			@SuppressWarnings("unchecked")
			Method m = c1.getDeclaredMethod(fieldName);
			String[] z = m.toString().split(" ");
			String fieldType = z[1];
			fieldType = Character.toUpperCase(fieldType.charAt(0)) + fieldType.substring(1);  //set 1st char touppercase
			if (fieldType.indexOf(".")>-1){ 
				String[] x = fieldType.split("\\.");
				fieldType = x[x.length - 1];
			}
			if (fieldType.equals("Int")) fieldType = "Integer";
			vo.setMGetter(m);
			vo.setIsMethod(true);
			vo.setFieldType(fieldType);
			//debug2("cfm - ft="+fieldType+" m="+m.toString()+" ism="+vo.getIsMethod());

		} catch (NullPointerException|NoSuchMethodException xx){ 
			debug2("cfm - fn="+fieldName+" "+xx.toString());
			vo.setIsMethod(false);
		}
		return vo;

	}
	//================================================
	public DRFindVO setFieldType(String fieldName, DRFindVO vo, DRListTBL<T> drl) 
	{
		String validTypes = "String|Integer|Short|Byte|Long|Boolean|BigDecimal|BigInteger|Character";
		String[] x = fieldName.split(" ");			//if asc/dsc at end of fieldname the sorted to true
		fieldName = x[0];
		if (x.length == 2){
			if (x[1].equals("asc")) vo.setSortAsc(true); 
			if (x[1].equals("dsc")) vo.setSortDsc(true);
		}
		debug1("vf - start fn="+fieldName+" siz="+drl.size);
		Class c1 = drl.dl.obj.getClass();
		Field f = null;
		try{
			f = c1.getDeclaredField(fieldName);
			vo.setFieldF1(f);
			f.setAccessible(true);

		}catch (NoSuchFieldException nsf){
			vo.setFieldType("");
			if (fieldName.equals("asc")) vo.setSortAsc(true);  
			if (fieldName.equals("dsc")) vo.setSortDsc(true);
			//debug2("st - fn="+fieldName+" c1="+c1.toString());
			if (fieldName.length() > 0) vo = checkForMethod(fieldName, vo, c1);
			fieldName = "";
		}
		String fieldType = "";
		boolean isArray = false;
		if (vo.getIsMethod()){
			fieldType = vo.getFieldType();
		} else if (fieldName.length() == 0){
			String[] z = c1.toString().split("\\.");
			fieldType = z[z.length - 1];
			if (validTypes.indexOf(fieldType) == -1) fieldType = "";
		}else{
			if (f.getType().isArray()){ 
				isArray = true;
    				Class componentType = f.getType().getComponentType();
				fieldType = componentType.toString();
			}else{
				fieldType = f.getType().toString();
				String[] z = fieldType.split("\\.");
				fieldType = z[z.length - 1];
			}
			fieldType = Character.toUpperCase(fieldType.charAt(0)) + fieldType.substring(1);  //set 1st char touppercase
			if (fieldType.equals("Int")) fieldType = "Integer";
			//debug2("st - ft=["+fieldType+"] c1="+c1.toString());
		}
		vo.setIsArray(isArray);
		vo.setFieldType(fieldType);
		vo.setFieldName(fieldName);
		return vo;
	}


	//================================================
	public DRFindVO validateDate(String fieldName, String value, DRFindVO vo, DRListTBL<T> drl, String op) 
		throws DRNoMatchException
	{
		DRLocalDateTime dldt = new DRLocalDateTime();
		vo = this.setFieldType(fieldName, vo, drl);

		if (!dldt.validFieldType(vo.getFieldType())) throw new DRNoMatchException("Valid date fieldtype not found");

		vo.setOrigFieldType("");

		//the field type is long now we need to check that the format is correct date format or an integer
		vo.setDateInterval(' ');
		String dateStr = "";
		if (dldt.checkTimeUnits(value.substring(value.length()-1))){		//last char is d,w,h,m,y etc 	
			//format <Date>:10d
			vo.setDateIntervalNum(Integer.parseInt(value.substring(value.indexOf("<Date>:")+7,value.length()-1)));
			dateStr = value.substring(value.indexOf("<Date>:")+7);
			if (vo.getFieldType().equals("LocalDate")) dateStr = dldt.getNewDate(dateStr).toStringLocalDate();
			else dateStr = dldt.getNewDate(dateStr).toStringDate();
			vo.setDateStr(dateStr);
			//debug2("drf - dstr="+dateStr+" int="+vo.getDateIntervalNum()+" val="+value);
		}else{ 								
			dateStr = value.substring(value.indexOf("<Date>:")+7, value.length());
			String dow = "";
			int weekno = 0;
			if (dateStr.length() > 2) dow = dateStr.substring(0,3);
			if (dldt.checkDayOfWeek(dow)){				//format Mon-19
				try{
					if (dateStr.length() > 3) weekno = Integer.parseInt(dateStr.substring(3,dateStr.length()));
				}catch (Exception e){
					throw new DRNoMatchException(e.getMessage()+" Invalid number with day of week - ["+dateStr+"]");
				}
				if (vo.getFieldType().equals("LocalDate")) 
					dateStr = dldt.getNewDate(dldt.getNewDayNum(dow,weekno).getDayDiff()+"d").toStringLocalDate();
				else dateStr = dldt.getNewDate(dldt.getNewDayNum(dow,weekno).getDayDiff()+"d").toStringDate();
				vo.setDateStr(dateStr);
			}else{							//format dd-mm-yyyy hh:mm:ss
				vo.setDateIntervalNum(0);
				if (vo.getFieldType().equals("LocalDate") && dateStr.indexOf(":") > -1){ 
					throw new DRNoMatchException("LocalDate contains time "+dateStr);
				}
				if (!vo.getFieldType().equals("LocalDate") && dateStr.indexOf(":") == -1) dateStr = dateStr + " 00:00:00";
				vo.setDateStr(dateStr); 
			}
		}
		this.createMethods(vo);
		if (dldt.validateDateTime(vo.getDateStr())){
			if (vo.getFieldType().equals("Long")){
				vo.setStartDate(dldt.setDateStr(vo.getDateStr()).toMillisec());
				setValue(vo.getStartDate() + "",vo); 
				vo.setVOrigString(vo.getStartDate() + "");
			}else{
				vo.setStartLDT(dldt.setDateStr(vo.getDateStr()).getDateLDT());
				setValue(vo.getDateStr(),vo); 
				vo.setVOrigString(vo.getDateStr());
			}
		}else{
			throw new DRNoMatchException("Date is not valid "+vo.getDateStr());
		}

		return vo;
	}
	//================================================
	public DRFindVO validateField(String fieldName, String value, DRFindVO vo, DRListTBL<T> drl, String op) 
		throws DRNoMatchException
	{
		vo = this.setFieldType(fieldName, vo, drl);
		if (vo.getFieldType().equals("")) throw new DRNoMatchException("Valid fieldtype not found for validateField");
		vo.setOrigFieldType("");

		//check all types of fields using invoke which returns a boolean
		op = op.toUpperCase();
		vo.setOpTxt(op);
		if (!op.equals("MIN") && !op.equals("MAX")){
			try{
				String methodName = "isValid"+vo.getFieldType();
				Class[] cArg = new Class[1]; 
				cArg[0] = String.class;
				ProcessTypeVO pt = new ProcessTypeVO();
  				Method m = pt.getClass().getDeclaredMethod(methodName,cArg);
				Object[] param = {value};
				Boolean ret = (Boolean) m.invoke(pt,param);
				if (!ret) throw new DRNoMatchException("Value parameter not suitable for field parameter "+vo.getFieldType());

			} catch (InvocationTargetException|IllegalAccessException|NullPointerException|NoSuchMethodException xx){ 
				throw new DRNoMatchException("Value parameter not suitable for field parameter "+xx.toString());
			}
		}
		vo = createMethods(vo);

		//debug2("vf - b4sv ft="+vo.getFieldType()+" fn="+vo.getFieldName());

		if (!value.equals("SORT")){
			setValue(value,vo);						
			vo.setVOrigString(value);						
		}
		//debug2("vf - end ft="+vo.getFieldType());
		return vo;
	}
	//================================================
	public DRFindVO createMethods(DRFindVO vo) throws DRNoMatchException
	{
		//define all methods used for later use as once we have field type we can define the methods 
		//once which are invoked later
		try{
			String methodName = "setO"+vo.getFieldType();
			Class[] cArg = new Class[1]; 
			cArg[0] = String.class;
 			Method m = vo.getClass().getDeclaredMethod(methodName,cArg);
			vo.setmSetO(m);

			methodName = "setV"+vo.getFieldType();
 			m = vo.getClass().getDeclaredMethod(methodName,cArg);
			vo.setmSetV(m);

			methodName = "getV"+vo.getFieldType();
 			m = vo.getClass().getDeclaredMethod(methodName);
			vo.setmGetV(m);

			methodName = "getO"+vo.getFieldType();
 			m = vo.getClass().getDeclaredMethod(methodName);
			vo.setmGetO(m);

			methodName = "compare"+vo.getFieldType();
 			m = vo.getClass().getDeclaredMethod(methodName);
			vo.setmCompare(m);

		} catch (NullPointerException|NoSuchMethodException xx){ 
			throw new DRNoMatchException("Error setting vo methods r "+xx.toString());
		}
		return vo;
	}
	//================================================
	public T[] DRFindObj(String fieldName, String operator, String value, DRListTBL<T> drl) 
		throws DRNoMatchException
	{
		DRFindVO vo = new DRFindVO();

		if (drl.fdl.obj == null) throw new DRNoMatchException("ListTbl is null");

		if (value.indexOf("<Date>:") > -1)
			vo = this.validateDate(fieldName, value, vo, drl, operator);
		else
			vo = this.validateField(fieldName, value, vo, drl, operator);
		T[] obj = this.DRFindInvoke(this.getOpCnt(operator), vo, drl);
		return obj;
	}
	//================================================
	public DRFindObjVO<T> DRFind(String fieldName, String operator, String value, DRListTBL<T> drl) 
		throws DRNoMatchException
	{
		DRFindVO vo = new DRFindVO();

		if (drl.fdl.obj == null) throw new DRNoMatchException("ListTbl is null");

		//check if date is being validated, this will have <Date>: in the value field
		if (value.indexOf("<Date>:") > -1)
			vo = this.validateDate(fieldName, value, vo, drl, operator);
		else
			vo = this.validateField(fieldName, value, vo, drl, operator);
		T[] obj = this.DRFindInvoke(this.getOpCnt(operator), vo, drl);

		DRFindObjVO<T> avo = new DRFindObjVO<T>();
		avo.setObjArray(obj);
		avo.setDRL(drl);
		return avo;
	}
	//================================================
	public DRFindObjVO<T> FindAnd(String fieldName, String operator, String value, T[] obj) 
		throws DRNoMatchException
	{
		DRCode<T> drc = new DRCode<T>();
		DRListTBL<T> ndrl = new DRListTBL<T>();
		DRFindVO vo = new DRFindVO();

		for (int i = 0; i < obj.length; i++) drc.DRadd(obj[i],ndrl);

		return this.DRFind(fieldName, operator, value, ndrl);
	}
	//================================================
	public DRFindObjVO<T> FindOr(String fieldName, String operator, String value, DRListTBL<T> drl, T[] obj) 
		throws DRNoMatchException
	{
		DRCode<T> drc = new DRCode<T>();
		DRFindObjVO<T> avo = new DRFindObjVO<T>();

		if (drl.fdl.obj == null) throw new DRNoMatchException("ListTbl is null");

		T[] obj1 = this.DRFindObj(fieldName, operator, value, drl);
		T[] obj2 = this.combineObjs(obj,obj1);
		avo.setObjArray(obj2);
		avo.setDRL(drl);
		return avo;
	}
	//================================================
	public DRFindObjVO<T> FindMinus(String fieldName, String operator, String value, T[] obj) 
		throws DRNoMatchException
	{
		DRCode<T> drc = new DRCode<T>();
		DRListTBL<T> ndrl = new DRListTBL<T>();
		DRFindVO vo = new DRFindVO();

		for (int i = 0; i < obj.length; i++)
			drc.DRadd(obj[i],ndrl);

		T[] obj1 = this.DRFindObj(fieldName, operator, value, ndrl);

		T[] obj2 = this.minusObjs(obj,obj1);
		DRFindObjVO<T> avo = new DRFindObjVO<T>();

		avo.setObjArray(obj2);
		return avo;
	}
	//================================================
	public T[] DRFindObject(String fieldName, String operator, String value, T[] obj) 
		throws DRNoMatchException
	{
		DRFind<T> drf = new DRFind<T>();
		DRCode<T> drc = new DRCode<T>();
		DRListTBL<T> ndrl = new DRListTBL<T>();
		DRFindVO vo = new DRFindVO();

		for (int i = 0; i < obj.length; i++)
			drc.DRadd(obj[i],ndrl);

		T[] obj1 = drf.DRFind(fieldName, operator, value, ndrl).getObjArray();

		return obj1;
	}
	//==============================================================
	public T[] minusObjs(T[] obj1, T[] obj2) throws DRNoMatchException
	{
	
		DRListTBL<T> dl1 = new DRListTBL<T>();
		DRCode<T> drc = new DRCode<T>();

		boolean matched = false;
		for (int i = 0; i < obj1.length; i++){
			matched = false;
			for (int j = 0; j < obj2.length; j++){
				if (obj1[i] == obj2[j]){
					matched = true;
					break;
				} 
			}
			if (!matched) drc.DRadd(obj1[i],dl1);
		}

		int size = drc.DRsize(dl1);
		debug1("f= - after comp siz="+size+" dl1o=");
		if (size == 0) throw new DRNoMatchException("DRFindMinus returns no objects");

		@SuppressWarnings("unchecked")
		T[] o = (T[]) java.lang.reflect.Array.newInstance(dl1.dl.obj.getClass(), size);

		o[0] = drc.DRgetFirst(dl1).dl.obj;
		for (int i = 1; i < size; i++)
			o[i] = drc.DRnext(dl1).dl.obj;

		debug1("f= - end ol="+o.length);
		return o;
	}

	//================================================
	public DRFindObjVO<T> distinct(T[] obj1) 
		throws DRNoMatchException
	{
		DRFind<T> drf = new DRFind<T>();
		DRCode<T> drc = new DRCode<T>();
		DRListTBL<T> ndrl = new DRListTBL<T>();
		DRFindVO vo = new DRFindVO();

		for (int i = 0; i < obj1.length - 1; i++){
			for (int j = i + 1; j < obj1.length; j++){
				if (checkFields(obj1[i],obj1[j])){
					obj1[i] = null;
					break;
				}
			}
			if (obj1[i] != null) drc.DRadd(obj1[i],ndrl);
		}
		drc.DRadd(obj1[obj1.length - 1],ndrl);

		@SuppressWarnings("unchecked")
		T[] nobj = (T[]) java.lang.reflect.Array.newInstance(ndrl.dl.obj.getClass(), drc.DRsize(ndrl));
		ndrl = drc.DRgetFirst(ndrl);
		for (int i = 0; i < nobj.length; i++){
			nobj[i] = ndrl.dl.obj;
			ndrl = drc.DRnext(ndrl);
		}

		DRFindObjVO<T> avo = new DRFindObjVO<T>();
		avo.setObjArray(nobj);

		return avo;
	}
	//================================================
	public boolean checkFields(T obj1, T obj2){

		boolean ret = false;
		Class c1 = obj1.getClass();
		Class c2 = obj2.getClass();
		if (c1 != c2) return false;
		String str1 = "";
		String str2 = "";
		int brcnt = 0;
		int i = 0;
		try{
			Field[] f1 = c1.getDeclaredFields();
			Field[] f2 = c2.getDeclaredFields();

			for (i = 0; i < f1.length; i++){
				try {
					f1[i].setAccessible(true);
					str1 = (String)f1[i].get(obj1).toString();
				} catch (NullPointerException npe) {
					str1 = "null";
				}
				try {
					f2[i].setAccessible(true);
					str2 = (String)f2[i].get(obj2).toString();
				} catch (NullPointerException npe) {
					str2 = "null";
				}
				if (str1.equals(str2)){
					//System.out.println("cf - i="+i+" s1="+str1+" s2="+str2+" f1l="+f1.length+" nam="+f1[i].getName());
					brcnt++;
				}
			}
			if (brcnt == i) ret = true;
			return ret;
		}catch (IllegalAccessException nsf){
			str1 = (String)obj1;
			str2 = (String)obj2;
			if (str1.equals(str2)) ret = true;
			return ret;
		}
	}
	//================================================
	public double getDouble(String op, String fieldName, T[] obj)
		throws DRNoMatchException
	{
		DRFind<T> drf = new DRFind<T>();
		DRListTBL<T> ndrl = new DRListTBL<T>();
		DRCode<T> drc = new DRCode<T>();
		DRFindVO vo = new DRFindVO();

		if (obj.length == 0) throw new DRNoMatchException("Object is null for getting average");

		for (int i = 0; i < obj.length; i++) drc.DRadd(obj[i],ndrl);

		vo = drf.setFieldType(fieldName, vo, ndrl);
		vo = createMethods(vo);

		double dval = 0.0d;
		for (int i = 0; i < obj.length; i++)
			dval += drf.getDoubleValue(obj[i],vo);

		if (op.equals("avg")) dval = dval / obj.length;

		return dval;		
	}
	//================================================
	public BigDecimal getBigDecimal(String op, String fieldName, T[] obj)
		throws DRNoMatchException
	{
		DRFind<T> drf = new DRFind<T>();
		DRListTBL<T> ndrl = new DRListTBL<T>();
		DRCode<T> drc = new DRCode<T>();
		DRFindVO vo = new DRFindVO();

		if (obj.length == 0) throw new DRNoMatchException("Object is null for getting BigDecimal average");

		for (int i = 0; i < obj.length; i++) drc.DRadd(obj[i],ndrl);

		vo = drf.setFieldType(fieldName, vo, ndrl);

		if (!vo.getFieldType().equals("BigDecimal") && !op.equals("sum"))
			throw new DRNoMatchException("Field type is not of type BigDecimal - "+vo.getFieldType());

		BigDecimal bigval = new BigDecimal("0.0");
		BigDecimal bigtot = new BigDecimal("0.0");
		for (int i = 0; i < obj.length; i++){
			T x = obj[i];
			try{
				if (vo.getFieldName().length() == 0){
					if (vo.getIsMethod()){
						bigval = new BigDecimal(vo.getMGetter().invoke(x).toString());
						//debug2("gdec - bv="+bigval);
					}else{
						bigval = new BigDecimal(x.toString());
					}
					bigtot = bigtot.add(bigval);
				}else{
					bigval = (BigDecimal) vo.getFieldF1().get(x);
					if (bigval==null) bigval = BigDecimal.valueOf(0); 
					bigtot = bigtot.add(bigval);
				}
			} catch (IllegalArgumentException|IllegalAccessException|InvocationTargetException iae) {
				debug1("sov - iae - "+vo.getFieldName());
				throw new DRNoMatchException("bigdec error for "+vo.getFieldName()+" "+iae.toString());
			}
		}
		if (op.equals("avg")) bigtot = bigtot.divide(new BigDecimal(obj.length),5);

		return bigtot;
	}
	//================================================
	public BigInteger getBigInteger(String op, String fieldName, T[] obj)
		throws DRNoMatchException
	{
		DRFind<T> drf = new DRFind<T>();
		DRListTBL<T> ndrl = new DRListTBL<T>();
		DRCode<T> drc = new DRCode<T>();
		DRFindVO vo = new DRFindVO();

		if (obj.length == 0) throw new DRNoMatchException("Object is null for getting BigInteger average");

		for (int i = 0; i < obj.length; i++) drc.DRadd(obj[i],ndrl);

		vo = drf.setFieldType(fieldName, vo, ndrl);

		if (!vo.getFieldType().equals("BigInteger") && !op.equals("sum"))
			throw new DRNoMatchException("Field type is not of type BigInteger");

		BigInteger bigval = new BigInteger("0");
		BigInteger bigtot = new BigInteger("0");
		for (int i = 0; i < obj.length; i++){
			T x = obj[i];
			try{
				if (vo.getFieldName().length() == 0){
					if (vo.getIsMethod()){
						bigval = new BigInteger(vo.getMGetter().invoke(x).toString());
						//debug2("gbig - bv="+bigval);
					}else{
						bigval = new BigInteger(x.toString());
					}
					bigtot = bigtot.add(bigval);
				}else{
					bigval = (BigInteger) vo.getFieldF1().get(x);
					if (bigval==null) bigval = BigInteger.valueOf(0); 
					bigtot = bigtot.add(bigval);
				}
			} catch (IllegalArgumentException|IllegalAccessException|InvocationTargetException iae) {
				debug1("sov - iae - "+vo.getFieldName());
				throw new DRNoMatchException("bigint exception for "+vo.getFieldName()+" "+iae.toString());
			}
		}
		if (op.equals("avg")) bigtot = bigtot.divide(new BigInteger(Integer.toString(obj.length)));

		return bigtot;
	}
	//================================================
	public long getLong(String op, String fieldName, T[] obj)
		throws DRNoMatchException
	{
		DRFind<T> drf = new DRFind<T>();
		DRListTBL<T> ndrl = new DRListTBL<T>();
		DRCode<T> drc = new DRCode<T>();
		DRFindVO vo = new DRFindVO();

		if (obj.length == 0) throw new DRNoMatchException("Object is null for getting average");

		for (int i = 0; i < obj.length; i++) drc.DRadd(obj[i],ndrl);

		vo = drf.setFieldType(fieldName, vo, ndrl);
		if (vo.getFieldType().equals("String") || vo.getFieldType().equals("Character")|| vo.getFieldType().equals("Boolean"))
			throw new DRNoMatchException("Field type is not of type numeric");
		vo = createMethods(vo);

		double dval = 0.0d;
		for (int i = 0; i < obj.length; i++)
			dval += drf.getDoubleValue(obj[i],vo);

		if (op.equals("avg")) dval = dval / obj.length;

		String dStr = Double.toString(dval);
		long lval = 0;
		if (vo.getFieldType().equals("BigDecimal")) lval  = BigDecimal.valueOf(dval).longValue();
		else lval = Long.parseLong(dStr.substring(0,dStr.indexOf(".")));

		return lval;
	}
	//===============================================================
	public double getDoubleValue(Object x, DRFindVO vo) throws DRNoMatchException
	{
		//debug("sov start - fn="+" fl=");
		double dval = 0.0d;
		String xstr = "";
		try{
			if (vo.getFieldName().length() == 0){ 
				if (vo.getIsMethod()){
					xstr = vo.getMGetter().invoke(x).toString();
					//debug2("gbig - bv="+bigval);
				}else{
					xstr = x.toString(); 
				}
			}else{ 
				xstr = vo.getFieldF1().get(x).toString();
			}
		} catch (IllegalArgumentException|IllegalAccessException|NullPointerException|InvocationTargetException iae) {
			debug2("gdv - xs="+xstr+" "+iae.toString());
			throw new DRNoMatchException("getDoubleF"+vo.getFieldType()+" - "+vo.getFieldName()+" "+iae.toString());
		}
		ProcessTypeVO pt = new ProcessTypeVO();
		dval = Double.parseDouble(xstr);
		return dval;
	}
	//================================================
	public T[] DRFindInvoke(int opCnt, DRFindVO vo, DRListTBL<T> drl) throws DRNoMatchException
	{
		String[] opTxt = {"Less","Equals","Greater","Like","Min","Max"};
		debug1("finv - start "+opCnt+" optxt="+opTxt[opCnt]);

		vo.setOpCnt(opCnt);
		DRFind<T> drf = new DRFind<T>();
		T[] obj;
		if (opTxt[opCnt].equals("Like")){
			obj = drf.DRFindLike(vo,drl);
		}else if (opTxt[opCnt].equals("Min")){
			try{
				String methodName = "compareValsLess";
				Class[] cArg = new Class[1]; 
				cArg[0] = Integer.class;
 				Method m = vo.getClass().getDeclaredMethod(methodName,cArg);
				vo.setmCompareVals(m);
				methodName = "compareArray"+opTxt[opCnt];
 				m = vo.getClass().getDeclaredMethod(methodName);
				vo.setmCompareArray(m);
		} catch (NullPointerException|NoSuchMethodException xx){ 
				throw new DRNoMatchException("Error declaring compareMin method "+xx.toString());
			}
			obj = drf.DRFindMinMax(vo,drl);
		}else if (opTxt[opCnt].equals("Max")){
			try{
				String methodName = "compareValsGreater";
				Class[] cArg = new Class[1]; 
				cArg[0] = Integer.class;
 				Method m = vo.getClass().getDeclaredMethod(methodName,cArg);
				vo.setmCompareVals(m);
				methodName = "compareArray"+opTxt[opCnt];
 				m = vo.getClass().getDeclaredMethod(methodName);
				vo.setmCompareArray(m);
			} catch (NullPointerException|NoSuchMethodException xx){ 
				throw new DRNoMatchException("Error declaring compareMax method "+xx.toString());
			}
			obj = drf.DRFindMinMax(vo,drl);
		}else{
			try{
				String methodName = "compareVals"+opTxt[opCnt];
				Class[] cArg = new Class[1]; 
				cArg[0] = Integer.class;
 				Method m = vo.getClass().getDeclaredMethod(methodName,cArg);
				vo.setmCompareVals(m);
				methodName = "compareArray"+opTxt[opCnt];
 				m = vo.getClass().getDeclaredMethod(methodName);
				vo.setmCompareArray(m);
			} catch (NullPointerException|NoSuchMethodException xx){ 
				throw new DRNoMatchException("Error declaring compareVals method optcnt="+opCnt+" "+xx.toString());
			}
			obj = drf.DRFindEqGtLt(vo,drl);
		}

		if (vo.getSortDsc() || vo.getSortAsc()) obj = sortObjs(obj,vo);
		debug1("f= - end ol="+obj.length);

		return obj;
	}

	//==============================================================
	public T[] DRFindEqGtLt(DRFindVO vo, DRListTBL<T> drl) throws DRNoMatchException
	{
	
		DRListTBL<T> dl1 = new DRListTBL<T>();
		DRCode<T> drc = new DRCode<T>();

		drl.dl = drl.fdl;

		setObjValue(drl.dl.obj,vo);

		debug1("fegl - start val="+getValue(vo));

		int j = 0;
		for (int i = 0; i < drc.DRsize(drl); i++){
			setObjValue(drl.dl.obj,vo);
			if (compareVals(vo)){
				drc.DRadd(drl.dl.obj,dl1);
			}
			drl = drc.DRnext(drl);
			if (drl.success == -1) break;
		}
		int size = drc.DRsize(dl1);
		debug1("f= - after comp siz="+size+" j="+j+" oval="+(String)getObjValue(vo));
		if (size == 0) throw new DRNoMatchException("No matches found for selection criteria oval="+(String)getObjValue(vo));

		@SuppressWarnings("unchecked")
		T[] o = (T[]) java.lang.reflect.Array.newInstance(dl1.dl.obj.getClass(), size);

		o[0] = drc.DRgetFirst(dl1).dl.obj;
		for (int i = 1; i < size; i++)
			o[i] = drc.DRnext(dl1).dl.obj;

		return o;
	}
	//==============================================================
	public T[] DRFindMinMax(DRFindVO vo, DRListTBL<T> drl) throws DRNoMatchException
	{
	
		DRListTBL<T> dl1 = new DRListTBL<T>();
		DRCode<T> drc = new DRCode<T>();
		DRArrayList<T> dl2 = new DRArrayList<T>();

		drl.dl = drl.fdl;

		dl2.obj = drl.dl.obj;
		setObjValue(drl.dl.obj,vo);
		setValue((String)getObjValue(vo),vo);
		debug1("fmin - start val="+getValue(vo));
		int j = 0;
		for (int i = 0; i < drc.DRsize(drl); i++){
			setObjValue(drl.dl.obj,vo);
			if (compareVals(vo)){
				setValue((String)getObjValue(vo),vo);
				dl2.obj = drl.dl.obj;
			}
			drl = drc.DRnext(drl);
			if (drl.success == -1) break;
		}
		if (dl2.obj == null) throw new DRNoMatchException("No min selection criteria oval="+(String)getObjValue(vo));

		@SuppressWarnings("unchecked")
		T[] o = (T[]) java.lang.reflect.Array.newInstance(drl.dl.obj.getClass(), 1);
		o[0] = dl2.obj;
		return o;
	}
	//==============================================================
	public T[] DRFindLike(DRFindVO vo, DRListTBL<T> drl) throws DRNoMatchException
	{
	
		DRListTBL<T> dl1 = new DRListTBL<T>();
		DRCode<T> drc = new DRCode<T>();

		drl.dl = drl.fdl;

		setObjValue(drl.dl.obj,vo);

		debug1("fegl - start val="+getValue(vo));

		int j = 0;
		for (int i = 0; i < drc.DRsize(drl); i++){
			setObjValue(drl.dl.obj,vo);
			if (compareLike(vo)){
				drc.DRadd(drl.dl.obj,dl1);
			}
			drl = drc.DRnext(drl);
			if (drl.success == -1) break;
		}
		int size = drc.DRsize(dl1);
		debug1("f= - after comp siz="+size+" j="+j+" oval="+(String)getObjValue(vo));
		if (size == 0) throw new DRNoMatchException("No matches found for selection criteria oval="+(String)getObjValue(vo));

		@SuppressWarnings("unchecked")
		T[] o = (T[]) java.lang.reflect.Array.newInstance(dl1.dl.obj.getClass(), size);

		o[0] = drc.DRgetFirst(dl1).dl.obj;
		for (int i = 1; i < size; i++)
			o[i] = drc.DRnext(dl1).dl.obj;

		return o;
	}
	//=========================================================
	public DRListTBL<T> DRsortNoKey(DRListTBL<T> drl,int asc,String sortName) throws DRNoMatchException{

		DRCode<T> drcode = new DRCode<T>();
		DRFind<T> drf = new DRFind<T>();
		DRFindVO vo = new DRFindVO();
		final DRFindVO fvo = vo;

		DRArrayList<T>[] xx = drcode.toArraySub(drl.dl,drl.fdl,drl.size);
		debug1("sank - xxl="+xx.length+" xsk="+xx[xx.length - 1].sortKey);

		vo = drf.setFieldType(sortName, vo, drl);
		if (vo.getFieldType().equals("")) throw new DRNoMatchException("Valid fieldtype not found for sortnokey");
		vo = createMethods(vo);

		final String ft = vo.getFieldType();
		final String fn = vo.getFieldName();

		Arrays.sort(xx, new Comparator<DRArrayList<T>>() {
			@Override
			public int compare(DRArrayList<T> dl1, DRArrayList<T> dl2) {
				if (compareObjs(dl1.obj, dl2.obj, fvo)==1){		// is dl1 > dl2 then true
					return asc;					// if asc = 1 then asc if -1 desc
				}else{
					return asc*-1;
				}
			}
		});

		drl = drcode.clear(drl);
		drl = drcode.toDRListsub(xx,drl);

		Arrays.fill(xx, null );				//clear down the array
		debug1("sank - size="+drl.size);

		return drl;
	}
	//==============================================================
	public DRFindObjVO<T> sortAsc(T[] obj,String fieldName) throws DRNoMatchException
	{
		DRFindVO vo = new DRFindVO();
		DRListTBL<T> ndrl = new DRListTBL<T>();
		DRCode<T> drc = new DRCode<T>();

		if (obj.length == 0) throw new DRNoMatchException("SortAsc - No elements to sort");

		for (int i = 0; i < obj.length; i++) drc.DRadd(obj[i],ndrl);

		//as value is null use min operator
		vo = this.validateField(fieldName, "SORT", vo, ndrl, "min");
		vo.setSortAsc(true);
		T[] nobj = sortObjs(obj,vo);
		DRFindObjVO<T> avo = new DRFindObjVO<T>();
		avo.setObjArray(nobj);

		return avo;
	}
	//==============================================================
	public DRFindObjVO<T> sortDsc(T[] obj,String fieldName) throws DRNoMatchException
	{
		DRFindVO vo = new DRFindVO();
		DRListTBL<T> ndrl = new DRListTBL<T>();
		DRCode<T> drc = new DRCode<T>();

		if (obj.length == 0) throw new DRNoMatchException("SortDsc - No elements to sort");

		for (int i = 0; i < obj.length; i++) drc.DRadd(obj[i],ndrl);

		//as hisis null use min operator
		vo = this.validateField(fieldName, "SORT", vo, ndrl, "min");
		vo.setSortDsc(true);

		T[] nobj = sortObjs(obj,vo);
		DRFindObjVO<T> avo = new DRFindObjVO<T>();
		avo.setObjArray(nobj);

		return avo;
	}
	//==============================================================
	public T[] sortObjs(T[] objs, DRFindVO vo) throws DRNoMatchException
	{

		//debug2("sob = asc="+vo.getSortAsc()+" dsc="+vo.getSortDsc()+" ol="+objs.length);

		final String ft = vo.getFieldType();
		final String fn = vo.getFieldName();

		//if array then set compareArray depending asc or dsc, asc/dsc is greater as the order is denoted by 
		//asc/dsc and max val
		if (vo.getIsArray()){
			try{
				String methodName = "compareArrayGreater";
 				Method m = vo.getClass().getDeclaredMethod(methodName);
				vo.setmCompareArray(m);
			} catch (NoSuchMethodException xx){throw new DRNoMatchException("sortObjs - "+xx.toString());}
		}
		final DRFindVO fvo = vo;
		int ascv = 0;
		if (vo.getSortAsc()) ascv = -1;
		if (vo.getSortDsc()) ascv = 1;
		final int asc = ascv;

		Arrays.sort(objs, new Comparator<T>() {
			@Override
			public int compare(T o1, T o2)
			{
				if (compareObjs(o1, o2, fvo)==1){	// is o1 > o2 then true
					return asc;			// if asc = 1 then asc if -1 desc
				}else{
					return asc*-1;
				}
			}
		});

		return objs;
	}
	//==============================================================
	public int compareObjs(T obj1, T obj2, DRFindVO fvo)
	{
		int greater = 0;
		try
		{
			setObjValue(obj1,fvo);
			setValue((String)getObjValue(fvo),fvo);
			setObjValue(obj2,fvo);

			try{
				if (fvo.getIsArray()){ 
					greater = (int)fvo.getmCompareArray().invoke(fvo);
					if (greater == 2) greater = (int)fvo.getmCompare().invoke(fvo);
				}else{
					greater = (int)fvo.getmCompare().invoke(fvo);
				}
			} catch (IllegalAccessException|InvocationTargetException|NullPointerException xx){ 
				System.out.println("compareObjs - "+xx.toString());
				return 0;
			}
			return greater;

		}catch (DRNoMatchException dnm){
			System.out.println("compareObjs - "+dnm.getMessage());
			return 0;
		}
	}
	//==============================================================
	public T[] combineObjs(T[] obj1, T[] obj2) throws DRNoMatchException
	{
	
		DRListTBL<T> dl1 = new DRListTBL<T>();
		DRCode<T> drc = new DRCode<T>();

		for (int i = 0; i < obj1.length; i++){
			for (int j = 0; j < obj2.length; j++){
				if (obj1[i] == obj2[j]) obj2[j] = null;
			}
			drc.DRadd(obj1[i],dl1);
		}
		for (int i = 0; i < obj2.length; i++)
			if (obj2[i] != null) drc.DRadd(obj2[i],dl1);

		int size = drc.DRsize(dl1);
		debug1("f= - after comp siz="+size+" dl1o=");
		if (size == 0) throw new DRNoMatchException("No objects to combine for FindOR");

		@SuppressWarnings("unchecked")
		T[] o = (T[]) java.lang.reflect.Array.newInstance(dl1.dl.obj.getClass(), size);

		o[0] = drc.DRgetFirst(dl1).dl.obj;
		for (int i = 1; i < size; i++)
			o[i] = drc.DRnext(dl1).dl.obj;

		debug1("f= - end ol="+o.length);
		return o;
	}
	//==============================================================
	public String getFieldType(String fieldName, T[] obj) throws DRNoMatchException
	{
		DRFind<T> drf = new DRFind<T>();
		DRListTBL<T> ndrl = new DRListTBL<T>();
		DRCode<T> drc = new DRCode<T>();
		DRFindVO vo = new DRFindVO();

		if (obj.length == 0) throw new DRNoMatchException("Object is null for getting field type");

		for (int i = 0; i < obj.length; i++) drc.DRadd(obj[i],ndrl);

		vo = drf.setFieldType(fieldName, vo, ndrl);

		return vo.getFieldType();
	}
	//==============================================================
	public String checkForArray(String fieldName){
		if (fieldName.indexOf("[") == -1){ 
			arrayNum = 0;
			return fieldName;
		}
		DRFindVO vo = new DRFindVO();
		String newFn = fieldName.substring(0,fieldName.indexOf("["));
		arrayNum = Integer.parseInt(fieldName.substring(fieldName.indexOf("[")+1,fieldName.indexOf("]")));
		//debug2("nf="+newFn+" an="+arrayNum);
		return newFn;
	}
	//==============================================================
	public String[] getFieldString(String fieldName, T[] obj) throws DRNoMatchException
	{
		DRFind<T> drf = new DRFind<T>();
		DRListTBL<T> ndrl = new DRListTBL<T>();
		DRCode<T> drc = new DRCode<T>();
		DRFindVO vo = new DRFindVO();

		if (obj.length == 0) throw new DRNoMatchException("Object is null for getting field type");

		for (int i = 0; i < obj.length; i++) drc.DRadd(obj[i],ndrl);
		vo = drf.setFieldType(fieldName, vo, ndrl);
		vo = drf.createMethods(vo);
			
		String[] xx = new String[obj.length];
		for (int i = 0; i < obj.length; i++){ 
			drf.setObjValue(obj[i],vo);
			if (vo.getIsArray()){
				String[] x = vo.getOArrayStr().split("!");
				//debug2("gfs - x="+x[arrayNum]+" i="+i);
				xx[i] = x[arrayNum];
			}else{
				xx[i] = (String)drf.getObjValue(vo);
			}
		}

		return xx;
	}
	//===============================================================
	public void setObjValue(Object x, DRFindVO vo) throws DRNoMatchException
	{
		String str = "";
		try{
			if (vo.getFieldName().length() == 0){
				str = x.toString();
				if (vo.getIsMethod()){
					str = vo.getMGetter().invoke(x).toString();
					//debug2("sov - isM str="+str);
				}
			}else{
				if (vo.getIsArray()){
					Object strA = vo.getFieldF1().get(x);
					Object ele = Array.get(strA,1);
					for (int i = 0; i < Array.getLength(strA); i++) str += Array.get(strA,i).toString() + "!";
					vo.setOArrayStr(str);
					//debug2("sov - str="+str+" len="+Array.getLength(strA));
				}else{
					DRLocalDateTime dldt = new DRLocalDateTime();
					if (vo.getFieldType().equals("LocalDateTime")){
						//this halves the execution time 
						LocalDateTime ldt = (LocalDateTime)vo.getFieldF1().get(x);
						str = dldt.setDateLDT(ldt).toStringDate();
					}else if (vo.getFieldType().equals("LocalDate")){
						LocalDate ld = ((LocalDate)vo.getFieldF1().get(x));
						str = dldt.setDateLocalDate(ld).toStringLocalDate();
						//debug2("sov - str="+str);
					}else if (vo.getFieldType().equals("Date")){
						Date ld = ((Date)vo.getFieldF1().get(x));
						str = dldt.setDateDate(ld).toStringDate();
					}else{
						str = vo.getFieldF1().get(x).toString();
					}
				}
			}
		}catch (IllegalArgumentException|IllegalAccessException|NullPointerException|InvocationTargetException nsm){
			debug2("sov - nsm "+vo.getFieldName()+" "+vo.getFieldType()+" "+vo.getIsMethod()+" "+nsm.toString());
			throw new DRNoMatchException("Error finding field - "+vo.getFieldName()+" "+nsm.toString());
		}
		try{
			Object[] param = {str};
			vo.getmSetO().invoke(vo,param);			//vo.setO<fieldType> ie setOInteger()
		} catch (IllegalAccessException|NullPointerException|InvocationTargetException xx){ 
			debug2("sov end - ft="+vo.getFieldType()+" str="+str+" fn="+vo.getFieldName()+" "+
					vo.getIsMethod()+" "+xx.toString());
			throw new DRNoMatchException("Error setObjValue variables "+xx.toString());
		}

	}
	//==================================================
	public Object getObjValue(DRFindVO vo)  throws DRNoMatchException {

		String retStr = "";
		try{
			retStr = (String)vo.getmGetO().invoke(vo);
			//debug2("sov end - ft="+vo.getFieldType()+" os="+vo.oString);

		} catch (IllegalAccessException|InvocationTargetException xx){ 
			throw new DRNoMatchException("Error getObjValue object variables "+xx.toString());
		} catch (NullPointerException npe){
			retStr = "0";
		}
		return retStr;
	}
	//============================================
	public void setValue(String x, DRFindVO vo) throws DRNoMatchException {
		//debug("sv - x="+x+" ft="+fieldType);

		if (x.length() == 0 && !vo.getFieldType().equals("String")) x = "0";
	
		try{
			Object[] param = {x};
			vo.getmSetV().invoke(vo,param);
			//debug2("sv - ft="+vo.getFieldType()+" x="+x);

		} catch (IllegalAccessException|NullPointerException|InvocationTargetException xx){ 
			throw new DRNoMatchException("Error setValue object variables "+xx.toString());
		}
	}
	//================================================	
	public Object getValue(DRFindVO vo)  throws DRNoMatchException {

		String retStr = "";
		try{
			retStr = (String)vo.getmGetV().invoke(vo);
			//debug2("sov end - ft="+vo.getFieldType());

		} catch (IllegalAccessException|InvocationTargetException xx){ 
			throw new DRNoMatchException("Error getting object variables "+xx.toString());
		} catch (NullPointerException npe){
			retStr = "0";
		}
		return retStr;
	}
	//=======================================================
	public Boolean compareVals(DRFindVO vo) throws DRNoMatchException {

		Boolean foundVal;
		try{
			int greater = 0;
			if (vo.getIsArray()){ 
				greater = (int)vo.getmCompareArray().invoke(vo);
				if (greater == 2) greater = (int)vo.getmCompare().invoke(vo);
			}else{
				greater = (int)vo.getmCompare().invoke(vo);
			}
			Object[] param = {greater};
			foundVal = (Boolean)vo.getmCompareVals().invoke(vo,param);

		} catch (IllegalAccessException|InvocationTargetException|NullPointerException xx){ 
			throw new DRNoMatchException("Error comparing object variables "+xx.toString());
		}

		return foundVal;
	}
	//=======================================================
	public boolean compareLike(DRFindVO vo) throws DRNoMatchException {

		String vstr = vo.getVOrigString();
		String ostr = (String)getObjValue(vo);

		int x1 = ostr.indexOf(vstr);
		return (x1 > -1);
	}
}