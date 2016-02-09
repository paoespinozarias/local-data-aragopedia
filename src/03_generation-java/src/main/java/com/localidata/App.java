package com.localidata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;




public class App {

	private final static Logger log = Logger.getLogger(App.class);

	static String inputDirectoryString = "";

	static String outputDirectoryString = "";

	static String configDirectoryString = "";

	static String propertiesFileString = "";

	String[] extensions = new String[] { "csv" };

	String[] extensionsConfig = new String[] { "xlsx", "csv" };
	HashMap<String,ConfigBean> mapconfig = new HashMap<String,ConfigBean>();
	private String formatConfig = "xlsx";
	
	public App(String input, String output, String config) {
		this.inputDirectoryString = input;
		this.outputDirectoryString = output;
		this.configDirectoryString = config;
	}

	public static void main(String[] args) {
		if ((log == null) || (log.getLevel() == null))
			PropertyConfigurator.configure("log4j.properties");
		if (args.length == 3) {
			log.info("Start process");
			App app = new App(args[0], args[1], args[2]);
			app.backup();
			app.extractConfig();
			app.extractInformation();
			log.info("Finish process");
		} else {
			log.info("Se deben de pasar dos parámetros: ");
			log.info("\tEl directorio donde están los archivos de entrada");
			log.info("\tEl directorio donde se generaranlos archivos ttl");
			log.info("\tEl directorio donde están los excel de configuación");
		}

	}
	
	private void extractConfig() {
		
		File configDirectoryFile = new File(configDirectoryString);
		Collection<File> listCSV = FileUtils.listFiles(configDirectoryFile,
				extensionsConfig, true);
		for (File file : listCSV) {
			if(!file.getName().startsWith("mapping")){
				ConfigBean configBean = new ConfigBean();
				configBean.setNameFile(file.getName());
				String id=file.getName().substring(8);
				id=id.replace(".csv", "");
				id=id.replace(".xlsx", "");
				id=id.replace("TC", "");
				id=id.replace("TM", "");
				id=id.replace("TP", "");
				id=id.replace("A", "");
				while(id.charAt(id.length()-1)=='-'){
					id=id.substring(0, id.length()-1);
				}
				
				configBean.setId(id);
				if(formatConfig.equals("csv"))
					readCsv(file, configBean);
				else
					readXlsx(file, configBean);
				mapconfig.put(id, configBean);
			}
		}
		
	}

	private void readCsv(File file, ConfigBean configBean) {
		
		List<String> csvLines;
		try {
			csvLines = FileUtils.readLines(file, "UTF-8");
			
			String[] cellsName = csvLines.get(0).split(",");
			String[] cellsNormalization = csvLines.get(1).split(",");
			String[] cellsDimMesure = csvLines.get(2).split(",");
			String[] cellsType = csvLines.get(3).split(",");
			String[] cellsSkosfile = csvLines.get(4).split(",");
			
			boolean cont=true;
			int columnReaded=0;
			while(cont && columnReaded<cellsName.length){
				DataBean dataBean = new DataBean();
				if(cellsName[columnReaded]==null){
					cont=false;
				}else{
					dataBean.setName(removeStartEndCaracter(cellsName[columnReaded]));
					dataBean.setNormalizacion(removeStartEndCaracter(cellsNormalization[columnReaded]));
					dataBean.setDimensionMesure(removeStartEndCaracter(cellsDimMesure[columnReaded]));
					dataBean.setType(removeStartEndCaracter(cellsType[columnReaded]));
					if(Utils.validValue(removeStartEndCaracter(cellsSkosfile[columnReaded]))){
						HashMap<String, SkosBean> mapSkos = processSkos(removeStartEndCaracter(cellsSkosfile[columnReaded]));
						dataBean.setMapSkos(mapSkos);
					}
					configBean.getMapData().put(Utils.urlify(dataBean.getName()), dataBean);
					columnReaded++;
				}
			}
		} catch (IOException e) {
			log.error("Error read csv ",e);
		}
		
		
	}

	private String removeStartEndCaracter(String csvLine) {
		String line=csvLine;
		if(csvLine!=null){
			if(csvLine.startsWith("\""))
				line =  csvLine.substring(1, csvLine.length());
			if(csvLine.endsWith("\""))
				line = line.substring(0, line.length()-1);
		}
		return line;
	}

	private void readXlsx(File file, ConfigBean configBean) {
		InputStream inp = null;
		Workbook wb = null;
		try {
			inp = new FileInputStream(file);
			wb = WorkbookFactory.create(inp);
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
		} catch (InvalidFormatException e) {
			log.error(e.getMessage());
		} catch (IOException e) {
			log.error(e.getMessage());
		}
		
		Sheet sheet = wb.getSheetAt(0);
		Row rowName = sheet.getRow(0);
		Row rowNormalization = sheet.getRow(1);
		Row rowDimMesure = sheet.getRow(2);
		Row rowType = sheet.getRow(3);
		Row rowSkosfile = sheet.getRow(4);
		boolean cont=true;
		int columnReaded=0;
		while(cont){
			Cell cellName = rowName.getCell(columnReaded);
			Cell cellNormalization = rowNormalization.getCell(columnReaded);
			Cell cellDimMesure = rowDimMesure.getCell(columnReaded);
			Cell cellType = rowType.getCell(columnReaded);
			Cell cellSkosfile = null;
			if(rowSkosfile!=null)
				cellSkosfile = rowSkosfile.getCell(columnReaded);
			DataBean dataBean = new DataBean();
			if(cellName==null){
				cont=false;
			}else{
				dataBean.setName(cellName.getStringCellValue());
				dataBean.setNormalizacion(cellNormalization.getStringCellValue());
				dataBean.setDimensionMesure(cellDimMesure.getStringCellValue());
				dataBean.setType(cellType.getStringCellValue());
				if(cellSkosfile!=null){
					HashMap<String, SkosBean> mapSkos = processSkos(cellSkosfile.getStringCellValue());
					dataBean.setMapSkos(mapSkos);
				}
				
				configBean.getMapData().put(Utils.urlify(dataBean.getName()), dataBean);
				columnReaded++;
			}
		}
	}

	private HashMap<String, SkosBean> processSkos(String skosPath) {
		HashMap<String, SkosBean> mapSkos = new HashMap<String, SkosBean>();
		
		File skosMappingg = new File(configDirectoryString+File.separator+skosPath);
		InputStream inp = null;
		Workbook wb = null;
		try {
			inp = new FileInputStream(skosMappingg);
			wb = WorkbookFactory.create(inp);
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
		} catch (InvalidFormatException e) {
			log.error(e.getMessage());
		} catch (IOException e) {
			log.error(e.getMessage());
		}
		if(wb!=null){
			Sheet sheet = wb.getSheetAt(0);
			for (int i = 0; i < sheet.getPhysicalNumberOfRows(); i++) {
				Row row = sheet.getRow(i);
				Cell cellId = row.getCell(0);
				Cell cellUri = row.getCell(1);
				SkosBean skosBean = new SkosBean();
				SkosBean skosBeanExtra = new SkosBean();
				String idCell = "";
				if(cellId.getCellType()==0){
					Double d = new Double(cellId.getNumericCellValue());
					idCell = d.intValue()+"";
				}else{
					idCell = cellId.getStringCellValue();
				}
				skosBean.setId(idCell);
				String uriCell = "";
				if(cellUri.getCellType()==0){
					Double d = new Double(cellUri.getNumericCellValue());
					uriCell = d.intValue()+"";
				}else{
					uriCell = cellUri.getStringCellValue();
					String id = uriCell.substring(uriCell.lastIndexOf("/")+1, uriCell.length()); 
					if(!idCell.equals(id)){
						skosBeanExtra.setId(id);
						skosBeanExtra.setURI(uriCell);
						mapSkos.put(id, skosBeanExtra);
					}
				}
				skosBean.setURI(uriCell);
				mapSkos.put(idCell, skosBean);				
			}
		}
		
		return mapSkos;
	}

	private void backup(){
		File outputDirectoryFile = new File(outputDirectoryString);
		if(outputDirectoryFile.exists()){
			SimpleDateFormat formatFullDate = new SimpleDateFormat("yyyyMMdd");
			String copy = outputDirectoryString+"_"+formatFullDate.format(new Date());
			File copyDirectoryFile = new File(copy);
			int aux = 1;
			while(copyDirectoryFile.exists()){
				copyDirectoryFile = new File(copy+"_"+aux++);
			}
			
			try {
				FileUtils.moveDirectoryToDirectory(outputDirectoryFile, copyDirectoryFile, true);
			} catch (IOException e) {
				log.error("Error haciendo backup",e);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void extractInformation() {
		// Getting input files
		File inputDirectoryFile = new File(inputDirectoryString);
		File propertiesFile = new File(outputDirectoryString + File.separator + "DatosTTL" + File.separator +"codelists"+ File.separator + "properties.ttl");
		File dsdFile = new File(outputDirectoryString + File.separator + "DatosTTL" + File.separator + "dataStructures" + File.separator + "dsd.ttl");
		File errorReportFile = new File(outputDirectoryString+ File.separator + "errorReport.txt");
		
		TransformToRDF.propertiesContent.append(TransformToRDF.addPrefix());
		TransformToRDF.dtdContent.append(TransformToRDF.addPrefix());
		
		Collection<File> listCSV = FileUtils.listFiles(inputDirectoryFile,
				extensions, true);
		String properties="";
		int numfile=1;
		// Transforming input files
		for (File file : listCSV) {
			try {
				String fileName = "";
				String fileLetter = "";
				if(file.getName().endsWith("A.csv")){
					fileName = file.getName().substring(0, file.getName().length()-5);
					fileLetter = file.getName().substring(file.getName().length()-5, file.getName().length()-4);
				}else{
					fileName = file.getName().substring(0, file.getName().length()-6);
					fileLetter = file.getName().substring(file.getName().length()-6, file.getName().length()-4);
				}
				ConfigBean configBean = mapconfig.get(fileName);
				File outputDirectoryFile = new File(outputDirectoryString+ File.separator + "DatosTTL" + File.separator + "informes" + File.separator + fileName+fileLetter + ".ttl");
				log.info("Init file " + fileName+fileLetter +". Size "+FileUtils.sizeOf(file)+" "+numfile+"/"+listCSV.size());
				List<String> csvLines = FileUtils.readLines(file, "UTF-8");
				TransformToRDF transformToRDF = new TransformToRDF(csvLines,outputDirectoryFile,propertiesFile,dsdFile,errorReportFile,configBean);
				transformToRDF.initTransformation(fileName+fileLetter,numfile,fileName);
				properties+=transformToRDF.getRdfProperties();

				log.info("End file " + outputDirectoryFile.getName()+" "+numfile+"/"+listCSV.size());
				numfile++;
			} catch (Exception e) {
				log.error("Error al extraer la información ",e);
			}
		}
		
		try {
			Utils.stringToFile(TransformToRDF.errorsReport,
					errorReportFile);
			Utils.stringToFile(TransformToRDF.propertiesContent.toString(), propertiesFile);
			Utils.stringToFile(TransformToRDF.dtdContent.toString(), dsdFile);
		} catch (Exception e) {
			log.error(e);
		}
	}
}
