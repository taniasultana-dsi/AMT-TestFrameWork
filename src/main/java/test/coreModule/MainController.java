package test.coreModule;

import org.junit.Test;
import org.openqa.selenium.WebDriver;
import test.utility.PropertyConfig;
import test.utility.ReadExcel;

import java.time.LocalDateTime;
import java.util.*;

public class MainController {
        final ClassLoader CLASS_LOADER = getClass().getClassLoader();
        private WebDriver webDriver;

        public MainController(){

        }
        public MainController(WebDriver driver){
            this.webDriver = driver ;
        }

    public TestPlan createTestPlanAndModule(){
        TestPlan testPlan = createTestPlan();
        List<TestModule> modules = testPlan.getAllTesModules();
        for(TestModule module : modules){
            if(module.getState().equals(PropertyConfig.INIT)){
                ReadExcel readExcel = new ReadExcel(CLASS_LOADER.getResource("modules/" + module.getModuleName() + ".xlsx").getPath());
                List<Map> records = readExcel.read(PropertyConfig.CONTROLLER);
                for (Map record : records) {
                    if(null == record.get(PropertyConfig.EXECUTION_FLAG) || record.get(PropertyConfig.EXECUTION_FLAG).toString().isEmpty()  || !record.get(PropertyConfig.EXECUTION_FLAG).toString().toLowerCase().equals("yes")  )
                        continue;
                    String sheetName = (String) record.get(PropertyConfig.SHEET_NAME);
                    String testCaseID = (String) record.get(PropertyConfig.TCID);
                    String testCaseName = (String) record.get(PropertyConfig.TEST_CASE_NAME);
                    TestSuite testSuite = module.getTestSuite(sheetName);
                    if(null ==  testSuite){
                        testSuite = new TestSuite(sheetName);
                        module.addTestSuite(testSuite);
                    }
                    TestCase testCase = new TestCase(testCaseID);
                    testCase.setTestCaseName(testCaseName);
                    testSuite.addTestCase(testCase);
                }
                module.setState(PropertyConfig.CREATED);
            }
        }
        return testPlan;
    }

    public void  createAndExecute() {
        TestPlan testPlan = createTestPlanAndModule();
        List<TestModule> modules = testPlan.getAllTesModules() ;
        for(TestModule testModule : modules){
            System.out.println(testModule.getModuleName());
            List<TestSuite>  testSuites = testModule.getAllTestSuits();
            for(TestSuite testSuite : testSuites){
                ReadExcel readExcel = new ReadExcel(CLASS_LOADER.getResource("modules/" + testModule.getModuleName() + ".xlsx").getPath());
                List<Map> records = readExcel.read(testSuite.getTestSuiteName());
                for(Map record : records) {
                    String testCaseNumber = (String) record.get(PropertyConfig.TC_ID);
                    testCaseNumber = testCaseNumber.split("\\.")[0];
                    TestCase testCase = testSuite.getTestCase(testCaseNumber);
                    if(null == testCase)
                        continue;
                    testCase.addTestStep(new TestStep(record));
                }
                executeTestesInTestSuite(testSuite);
                }
            }
        }

        public void executeTestesInTestSuite(TestSuite testSuite){

            webDriver.navigate().to("https://qa3.testamt.com/");
            webDriver.manage().window().maximize();
           List<TestCase> testCases = testSuite.getAllTestCases();
           ExecuteTests executeTests = new ExecuteTests(webDriver);
           for(TestCase testCase : testCases){
               executeTests.executeTest(testCase);
           }
            closeAlltabs(webDriver);
        }

    public  void closeAlltabs(WebDriver webDriver) {
        try {
            Set<String> windows = webDriver.getWindowHandles();
            Iterator<String> iter = windows.iterator();
            String[] winNames=new String[windows.size()];
            int i=0;
            while (iter.hasNext()) {
                winNames[i]=iter.next();
                i++;
            }

            if(winNames.length > 1) {
                for(i = winNames.length; i > 1; i--) {
                    webDriver.switchTo().window(winNames[i - 1]);
                    webDriver.close();
                }
            }
            webDriver.switchTo().window(winNames[0]);
            //webDriver.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }



    public TestPlan createTestPlan(){
        long start = System.currentTimeMillis();
        ReadExcel readExcel = new ReadExcel(CLASS_LOADER.getResource("testPlan/" + PropertyConfig.MODULE_CONTROLLER + ".xlsx").getPath());
        List<Map> records = readExcel.read(PropertyConfig.MODULE_CONTROLLER);
        TestPlan testPlan = new TestPlan();
        testPlan.setTestPlanName(LocalDateTime.now().toString());

        for(Map map : records) {
            String  moduleName = (String) map.get(PropertyConfig.MODULE_NAME);
            String executionFlag = (String) map.get(PropertyConfig.EXECUTION_FLAG);
            if ( null == moduleName || moduleName.isEmpty())
                continue;
            if ( null == executionFlag || !executionFlag.toLowerCase().equals("yes"))
                continue;
            TestModule testModule = new TestModule(moduleName);
            testPlan.addTestModule(testModule);
            }
        return testPlan ;
        }
    }