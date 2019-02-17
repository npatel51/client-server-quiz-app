import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

class DB {
    private JSONArray data;
    private Map<String, JSONObject> questions = new HashMap<String, JSONObject>();
    private Map<String, JSONArray> tags = new HashMap<String, JSONArray>();
    private Map<String, String> qtag = new HashMap<String, String>();
    private long questionID = -1;
    private Random random = new Random();

    public DB() {
        JSONParser parser = new JSONParser();
        try {
            Object payload = parser.parse(new FileReader(
                    "./assets/qbank.json"));
            data = (JSONArray) payload;
            jsonToMap();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    // parsing JSON to Map
    private void jsonToMap() {
        String maxKey = "0";
        for (Object datum : data) {
            JSONObject qtagobj = (JSONObject) datum;                       // question tag object
            String questionTag = (String) qtagobj.keySet().iterator().next();      // question tag
            JSONArray questionsArray = (JSONArray) qtagobj.get(questionTag);
            tags.put(questionTag, questionsArray);
            for (int j = 0; j < questionsArray.size(); ++j) {
                JSONObject jsonObj = (JSONObject) questionsArray.get(j);
                String questionKey = (String) jsonObj.keySet().iterator().next();   // question id
                JSONObject questionObj = (JSONObject) jsonObj.get(questionKey);      // question json object
                questions.put(questionKey, questionObj);
                qtag.put(questionKey, questionTag + ":" + j);                       // embedding tag and index for quick deletion
                if (compareString(questionKey, maxKey) > 0) {                             // used later to assign unique id to question
                    maxKey = questionKey;
                }
            }
        }
        questionID = Long.parseLong(maxKey);  // max key assigned in question bank so far

    }

    /**
     * Compare two numerical string
     *
     * @param a
     * @param b
     * @return -- 1 if a > b, 0 if a == b, -1 a < b
     */
    private int compareString(String a, String b) {
        if (a.length() > b.length()) {  // string a is bigger
            return 1;
        }
        if (a.length() < b.length()) {
            return -1;                // string a is smaller
        }
        for (int i = 0; i < a.length(); ++i) {
            if (a.charAt(i) > b.charAt(i)) return 1;
            if (a.charAt(i) < b.charAt(i)) return -1;
        }
        return 0;

    }

    /**
     * The methods of the DB class are synchronized to prevent threads interference and memory consistency errors
     * as DB object is shared. At any instance of time only one client will have access to database, all other client
     * will be blocked until the request the request for a client is handled i.e. this methods cannot be executed simultaneously
     */


    public synchronized String getRandomQuestion() {
        List<String> qNumbers = new ArrayList<>(questions.keySet());
        String randomQuestionNumber = qNumbers.get(random.nextInt(qNumbers.size()));
        return randomQuestionNumber + "\n" + questions.get(randomQuestionNumber).get("question");
    }

    public synchronized String deleteQuestion(String questionNumber) {
        if (questions.get(questionNumber) == null) {
            return "Question " + questionNumber + " not found!";
        }
        String[] params = qtag.get(questionNumber).split(":");
        System.out.println(params[0] + " " + params[1]);
        System.out.println(questionNumber);
        tags.get(params[0]).remove(Integer.parseInt(params[1]));     // delete from the collection of question under a tag
        questions.remove(questionNumber);             // delete question from map
        writeToDB();                                  // write back to the JSON file
        return "Question " + questionNumber + " was deleted successfully!";
    }

    public synchronized String getQuestion(String questionNumber) {
        if (questions.get(questionNumber) == null) {
            return "Question " + questionNumber + " not found!";
        }
        return (String) questions.get(questionNumber).get("question");
    }

    public synchronized String getAnswer(String questionNumber) {
        if (questions.get(questionNumber) == null) {
            return "Question " + questionNumber + " not found!";
        }
        return (String) questions.get(questionNumber).get("answer");
    }

    public synchronized String addQuestion(String[] params) {
        JSONObject jsonObject = new JSONObject();
        String questionKey = Long.toString(++questionID);
        String questionTag = params[0];
        jsonObject.put("question", params[1]);
        jsonObject.put("answer", params[2]);
        JSONObject question = new JSONObject();
        question.put(questionKey, jsonObject);
        if (!tags.containsKey(questionTag))
            tags.put(questionTag, new JSONArray());
        tags.get(questionTag).add(question);     //add question under the appropriate tag
        questions.put(questionKey, jsonObject);
        qtag.put(questionKey, questionTag + ":" + (tags.get(questionTag).size() - 1));
        writeToDB();
        return questionKey;
    }

    private void writeToDB() {
        // write map data to json file
        JSONArray data = new JSONArray();
        // convert map to json objects
        for (String key : tags.keySet()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(key, tags.get(key));
            data.add(jsonObject);
        }
        try (FileWriter file = new FileWriter("./assets/qbank.json")) {
            file.write(data.toJSONString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
