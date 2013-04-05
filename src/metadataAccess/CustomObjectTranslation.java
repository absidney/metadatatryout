package metadataAccess;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.PackageTypeMembers;
import com.sforce.soap.metadata.RetrieveRequest;
import com.sforce.soap.metadata.RetrieveResult;


public class CustomObjectTranslation {
	private static Double API_VERSION = 27.0;
	
	public static void main(String[] args) throws Exception  {
		File unpackedManifest = new File("conf\\package.xml");
		
		RetrieveRequest retrieveRequest = new RetrieveRequest();
		retrieveRequest.setUnpackaged(parsePackageManifest(unpackedManifest));
		retrieveRequest.setApiVersion(API_VERSION);
		MetadataConnection conn = MetadataLoginUtil.login();
		AsyncResult aresult = conn.retrieve(retrieveRequest);
		
		int poll = 0;
        long waitTimeMilliSecs = 1000;
        while (!aresult.isDone()) {
            Thread.sleep(waitTimeMilliSecs);
            waitTimeMilliSecs *= 2;
            if (poll++ > 20) {
                throw new Exception("I will wait no longer");
            }
            aresult = conn.checkStatus(new String[] {aresult.getId()})[0];
            System.out.println("Status is: " + aresult.getState());
        }
		
		RetrieveResult result = conn.checkRetrieveStatus(aresult.getId());
		System.out.println("Writing results to zip file");
        File resultsFile = new File("result\\translations.zip");
        FileOutputStream os = new FileOutputStream(resultsFile);
        try {
            os.write(result.getZipFile());
        } finally {
            os.close();
        }
	}

	private static com.sforce.soap.metadata.Package parsePackageManifest(File file)
            throws ParserConfigurationException, IOException, SAXException {
        com.sforce.soap.metadata.Package packageManifest = null;
        List<PackageTypeMembers> listPackageTypes = new ArrayList<PackageTypeMembers>();
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputStream inputStream = new FileInputStream(file);
        Element d = db.parse(inputStream).getDocumentElement();
        for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c instanceof Element) {
                Element ce = (Element) c;
                NodeList nodeList = ce.getElementsByTagName("name");
                if (nodeList.getLength() == 0) {
                    continue;
                }
                String name = nodeList.item(0).getTextContent();
                NodeList m = ce.getElementsByTagName("members");
                List<String> members = new ArrayList<String>();
                for (int i = 0; i < m.getLength(); i++) {
                    Node mm = m.item(i);
                    members.add(mm.getTextContent());
                }
                PackageTypeMembers packageTypes = new PackageTypeMembers();
                packageTypes.setName(name);
                packageTypes.setMembers(members.toArray(new String[members.size()]));
                listPackageTypes.add(packageTypes);
            }
        }
        packageManifest = new com.sforce.soap.metadata.Package();
        PackageTypeMembers[] packageTypesArray =
                new PackageTypeMembers[listPackageTypes.size()];
        packageManifest.setTypes(listPackageTypes.toArray(packageTypesArray));
        packageManifest.setVersion(API_VERSION + "");
        return packageManifest;
    }
	
}