package com.accolite.au;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import javax.xml.parsers.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class LicenseLine{
	public String licenseLine = "-";
	public Date effectiveDate  = null;
	public Date expiryDate = null;
	public String status = "-";
}

class License{
	public String stateCode;
	public String licenseNumber;
	public String effectiveDate;
	public String residentIndicator;
	public String licenseClass;
	public String expiryDate;
	public String licenseStatus	;
	boolean isLicenseLine = false;
	
	List<LicenseLine> License_Line_arr = new ArrayList<LicenseLine>();

	License(String StateCode , String license_no , String date){
		this.stateCode = StateCode;
		this.licenseNumber = license_no;
		this.effectiveDate = date;
	}
	
	@Override
	public String toString() {
		return " StateCode: " + stateCode + " " + " License Number: " + licenseNumber + " Effective Date: " + effectiveDate;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((effectiveDate == null) ? 0 : effectiveDate.hashCode());
		result = prime * result + ((stateCode == null) ? 0 : stateCode.hashCode());
		result = prime * result + ((licenseNumber == null) ? 0 : licenseNumber.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		License other = (License) obj;
		if (effectiveDate == null) {
			if (other.effectiveDate != null)
				return false;
		} else if (!effectiveDate.equals(other.effectiveDate))
			return false;
		if (stateCode == null) {
			if (other.stateCode != null)
				return false;
		} else if (!stateCode.equals(other.stateCode))
			return false;
		if (licenseNumber == null) {
			if (other.licenseNumber != null)
				return false;
		} else if (!licenseNumber.equals(other.licenseNumber))
			return false;
		return true;
	}
}

public class XmlParser {

	HashMap<String, List<License>> CSR_Map = new HashMap<>();
	HashMap<String, List<License>> licensesMap = new HashMap<>();
	private static final String LICENSE_ROW = "nipr,License ID,Jurisdiction,Resident,License Class,License Effective Date,License Expiry Date,License Status,License Line,License Line Effective Date,License Line Expiry Date,License Line Status";

	void parse(String filepath, HashMap<String, List<License>> tempMap, boolean isLine) {
		File Xml_file = new File(filepath);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
			Document doc = db.parse(Xml_file);
			doc.getDocumentElement().normalize();
			NodeList nodeList = doc.getElementsByTagName("CSR_Producer");
			for(int i = 0;i < nodeList.getLength();i++) {
				Node node = nodeList.item(i);
				if(node.getNodeType() == Node.ELEMENT_NODE) {
					Element ele = (Element)node;
					NodeList licenseNodeList = ele.getElementsByTagName("License");
					List<License> arr = new ArrayList<License>();
					for(int j = 0;j < licenseNodeList.getLength();j++) {
						Node tempNode = licenseNodeList.item(j);
						if(tempNode.getNodeType() == Node.ELEMENT_NODE) {
							Element temp_ele = (Element)tempNode;
							load_arr(arr ,temp_ele , isLine);
						}
					}
					tempMap.put(ele.getAttribute("NIPR_Number") , arr);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("deprecation")
	void load_arr(List<License> arr , Element temp_ele , boolean isLine) {
		License obj = new License(temp_ele.getAttribute("State_Code") , temp_ele.getAttribute("License_Number") , temp_ele.getAttribute("Date_Status_Effective"));
		obj.isLicenseLine = isLine;
		obj.residentIndicator = temp_ele.getAttribute("Resident_Indicator");
		obj.licenseClass = temp_ele.getAttribute("License_Class");
		obj.expiryDate = temp_ele.getAttribute("License_Expiration_Date");
		obj.licenseStatus = temp_ele.getAttribute("License_Status");
		if(isLine) {
			NodeList nodeList = temp_ele.getElementsByTagName("LOA");
			for(int i = 0;i < nodeList.getLength();i++) {
				Node temp_node = nodeList.item(i);
				LicenseLine object = new LicenseLine();
				if(temp_node.getNodeType() == Node.ELEMENT_NODE) {
					Element ele = (Element)temp_node;
					try {
						object.effectiveDate = new SimpleDateFormat("MM/dd/yyyy").parse(ele.getAttribute("LOA_Issue_Date"));
						object.expiryDate = object.effectiveDate;
						object.expiryDate.setYear(object.effectiveDate.getYear() + 2);
					} catch (ParseException e) {
						object.effectiveDate = null;
						object.expiryDate = new Date(0);
					}
					object.licenseLine = ele.getAttribute("LOA_Name");
					object.status = ele.getAttribute("LOA_Status");
				}
				obj.License_Line_arr.add(object);
			}
		}
		arr.add(obj);
	}

	void compareFiles() {
		boolean flag = true;
		for (Entry<String, List<License>> CSREntry : CSR_Map.entrySet()) {
			List<License> Main_arr = CSREntry.getValue();
			for (Entry<String, List<License>> licensesEntry : licensesMap.entrySet()) {
				List<License> arr = licensesEntry.getValue();
				if(CSREntry.getKey().equals(licensesEntry.getKey())) {
					for (License obj : Main_arr) {
						for(License temp_obj : arr) {
							if(obj.licenseNumber.equals(temp_obj.licenseNumber) &&
									(obj.stateCode.equals(temp_obj.stateCode)) &&
									(obj.effectiveDate.equals(temp_obj.effectiveDate))) {
								writeRow(licensesEntry.getKey() , licensesEntry.getValue(),"Merged.csv");
							}
						}
					}
					flag = false;
				} 
			}
			if(flag) {
				writeRow(CSREntry.getKey() , CSREntry.getValue(),"Invalid_Licenses.csv");
				CSREntry.setValue( null);
			}
		}
		for (Entry<String, List<License>> licensesEntry : licensesMap.entrySet()) {
			writeRow(licensesEntry.getKey() , licensesEntry.getValue(),"Invalid_License_Lines.csv");
		}
	}

	private void writeRow(String NIPR,List<License> arr , String File_Name) {

		FileWriter fw = null;
		try {
			fw = new FileWriter(File_Name);
			fw.append(LICENSE_ROW + "\n");
			for(License obj :arr) {
				if(obj.License_Line_arr.size() > 0) {
					for(int i = 0;i < obj.License_Line_arr.size();i++) {
						fw.append(NIPR + "," + obj.licenseNumber + ",");
						fw.append(obj.stateCode + "," + obj.residentIndicator + "," + obj.licenseClass + "," + obj.effectiveDate + "," + obj.expiryDate + "," + obj.licenseStatus + "," + obj.License_Line_arr.get(i).licenseLine + "," + obj.License_Line_arr.get(i).effectiveDate + "," + obj.License_Line_arr.get(i).expiryDate + "," + obj.License_Line_arr.get(i).status + "\n");
					}
				}	
				else {
					fw.append(NIPR + "," + obj.licenseNumber + "," + obj.stateCode + "," + obj.residentIndicator + "," + obj.licenseClass + "," + obj.effectiveDate + "," + obj.expiryDate + "," + obj.licenseStatus + "\n");
				}
			}		
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		try {
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		XmlParser parser = new XmlParser();
		parser.parse("C:\\Users\\Hyderabad-Intern\\Desktop\\MiniAU\\advancejava\\XMLParser\\CSRData.xml" , parser.CSR_Map , false);
		parser.parse("C:\\Users\\Hyderabad-Intern\\Desktop\\MiniAU\\advancejava\\XMLParser\\LicensesData.xml" , parser.licensesMap , true);		
		parser.compareFiles();
	}
}
