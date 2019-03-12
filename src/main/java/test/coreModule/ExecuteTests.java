package test.coreModule;

import org.openqa.selenium.WebDriver;
import test.Log.CreateLog;
import test.Log.LogMessage;
import test.keywordScripts.UIBase;
import test.keywordScripts.UtilKeywordScript;
import test.utility.PropertyConfig;
import test.utility.ReadExcel;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExecuteTests {

    private WebDriver webDriver;
    public ExecuteTests(WebDriver driver) {
        webDriver = driver ;
    }
    public void readAndExecute(String fileName,String sheetName) {
        ClassLoader classLoader = getClass().getClassLoader();
        long start = System.currentTimeMillis();
        ReadExcel readExcel = new ReadExcel(classLoader.getResource("modules/" + fileName + ".xlsx").getPath());
        List<Map> records = readExcel.read(sheetName);
        CreateLog createLog = new CreateLog("sampleReport");
        createLog.createLogger("sample test");
        for(Map map : records) {
            ArrayList<Object> objects = new ArrayList<Object>();
           // objects.add(webDriver);
            String actionName = (String) map.get(PropertyConfig.ACTION);
            String objectLocators = (String) map.get(PropertyConfig.OBJECT_LOCATORS);
            String testData = (String) map.get(PropertyConfig.TEST_DATA);
            String executionFlag = (String) map.get(PropertyConfig.EXECUTION_FLAG);
            String critical = (String) map.get(PropertyConfig.CRITICAL);
            int numberOfParams = 0;

            if ( null == executionFlag || !executionFlag.toLowerCase().equals("yes"))
                continue;
            if(null != objectLocators && ! objectLocators.isEmpty()) {
                objects.add(objectLocators);
                numberOfParams++;
            }
            if(null != testData && ! testData.isEmpty()){
                objects.add(testData);
                numberOfParams++;
            }
            LogMessage logMessage =  invokeMethod(actionName.split("\\.")[0],actionName.split("\\.")[1],numberOfParams,objects.toArray());
            createLog.writeLog("sample test",logMessage.getLogMessage(),logMessage.isPassed());
        }

    }
    public List<LogMessage> executeTest(TestCase testCase) {
        UIBase uiBase = new UIBase(webDriver);
        ClassLoader classLoader = getClass().getClassLoader();
        long start = System.currentTimeMillis();
        List<LogMessage> logMessages = new ArrayList<LogMessage>() ;
        List<TestStep> testSteps = testCase.getAllTestSteps();
        testCase.setPassed(true);
        for(TestStep testStep : testSteps) {
            ArrayList<Object> objects = new ArrayList<Object>();
            // objects.add(webDriver);
            String actionName = testStep.getAction();
            String objectLocators = testStep.getObjectLocator();
            String testData = testStep.getTestData();
            Boolean executionFlag = testStep.isExecutionFlagOn();
            Boolean pageRefresh = testStep.isRefreshPageOn();
            Boolean critical = testStep.isCritical();

            int numberOfParams = 0;

            if (! executionFlag) {
                LogMessage logMessage = new LogMessage(true,testStep.getTestStepDescription() + " --" + testStep.getFieldName() + "(Skipped)");
                //logMessage.setSkippedTrue();
                logMessages.add(logMessage)  ;
                continue;
            }
            if(null != objectLocators && ! objectLocators.isEmpty()) {
                objects.add(objectLocators);
                numberOfParams++;
            }
            if(null != testData && ! testData.isEmpty()){
                objects.add(testData);
                numberOfParams++;
            }
            UtilKeywordScript.delay(PropertyConfig.WAIT_TIME_SECONDS);
            LogMessage logMessage =  invokeMethod(actionName.split("\\.")[0],actionName.split("\\.")[1],numberOfParams,objects.toArray());
          //  UtilKeywordScript.delay(5);
            logMessage.setLogMessage(testStep.getTestStepDescription() + " --" + testStep.getFieldName() + "--" + logMessage.getLogMessage() );
            logMessages.add(logMessage);
            if(!logMessage.isPassed()) {
                testCase.setPassed(false);
                if(critical)
                    return logMessages;
            }

          if (pageRefresh){
                uiBase.WaitingForPageLoad();
          }
        }

        return logMessages;

    }

    public LogMessage invokeMethod(String className,String methodName,int numberOfParams,Object[] object) {
        try {

            Class<?> callingClass = Class.forName("test.keywordScripts." + className);
            Method callingMethod ;
            //System.out.println(numberOfParams);
            if(numberOfParams == 0)
                callingMethod = callingClass.getDeclaredMethod(methodName);
            else if(numberOfParams == 1)
                callingMethod = callingClass.getDeclaredMethod(methodName,String.class );
            else if(numberOfParams == 2)
                callingMethod = callingClass.getDeclaredMethod(methodName,String.class,String.class);
            else
                return new LogMessage(false, "number of parameter exceeds");
            Constructor<?> constructor = callingClass.getConstructor(WebDriver.class);
            LogMessage logMessage = (LogMessage) callingMethod.invoke(constructor.newInstance(webDriver),object);
            return logMessage;
        } catch(Exception ex) {
               ex.printStackTrace();
               return  new LogMessage(false,"exception occured");
        }
    }
}
