package com.rpi.soybot.database;

import org.json.JSONArray;
import org.json.JSONObject;

import com.rpi.soybot.Util;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Database {

    private Sqlite sqlite;
    private HashMap<String, List<CachedUserAnswer>> cachedAnswersByUser = new HashMap<>();

    private HashMap<String, List<String>> cachedRemainingQuestionIdsByUser = new HashMap<>();

    private List<String> allQuestionIds = new ArrayList<>();

    //questionId, question
    private HashMap<String, Question> cachedQuestions = new HashMap<String, Question>();

    //the score for users who haven't finished the quiz - TODO make them retake quiz to get score if theres new questions
    public static int SCORE_FOR_NOT_FINISHED = -10000;

    public Database(Sqlite sqlite) throws SQLException, IOException {
        this.sqlite = sqlite;
        this.setupDefaultQuestions();
        this.cacheAllQuestionIds();
        this.buildQuestionCache();
    }

    public Question getCurrentQuestionForUser(String userId) throws SQLException {
        PreparedStatement preparedStatement = this.sqlite.getStatement("SELECT * FROM UserMiscData WHERE userid = ?");
        preparedStatement.setString(1, userId);

        ResultSet resultSet = preparedStatement.executeQuery();

        while(resultSet.next()) {
            Question question = this.cachedQuestions.get(resultSet.getString("currentQuestionId"));
            return question;
        }

        return null;
    }

    public void increaseCurrentQuestionForUser(String userId) throws SQLException {
        this.sqlite.getStatement("UPDATE UserMiscData SET currentQuestionId = currentQuestionId + 1 WHERE userId = '" + userId + "'").execute();
    }

    public boolean userRegistered(String userId) throws SQLException {
        return this.sqlite.getStatement("SELECT * FROM UserMiscData WHERE userId = '" + userId + "'").executeQuery().next();
    }

    public void registerUser(String userId) throws SQLException {
        if(!this.userRegistered(userId)) {
            PreparedStatement preparedStatement = this.sqlite.getStatement("INSERT INTO UserMiscData (userId, currentQuestionId, currentScore) VALUES (?, ?, ?)");
            preparedStatement.setString(1, userId);
            preparedStatement.setInt(2, 0);
            preparedStatement.setInt(3, Database.SCORE_FOR_NOT_FINISHED);
            preparedStatement.execute();
        }
    }

    public boolean userHasAdditionalQuestionsAvailable(String userId) throws SQLException {
        if(this.cachedRemainingQuestionIdsByUser.get(userId) == null) {
            this.buildRemainingQuestionsCacheForUser(userId);
        }

        System.out.println("Cached remaining question ids for user:"   + this.cachedRemainingQuestionIdsByUser.get(userId).size());
        return this.cachedRemainingQuestionIdsByUser.get(userId).size() > 0;
    }

    public int getRemainingQuestionsForUser(String userId) throws SQLException {
        if(this.cachedRemainingQuestionIdsByUser.get(userId) == null) {
            this.buildRemainingQuestionsCacheForUser(userId);
        }

        return this.cachedRemainingQuestionIdsByUser.get(userId).size();
    }

    public void setAnswerForUserCurrentQuestion(String userId, String username, boolean answer) throws SQLException {
        String questionId = this.getCurrentQuestionForUser(userId).getQuestionId();
        String question = this.getCurrentQuestionForUser(userId).getQuestion();

        this.setAnswerForUser(userId, username, questionId, question, answer);

        this.increaseCurrentQuestionForUser(userId);
    }
    public void setAnswerForUser(String userId, String username, String questionId, String question, boolean answer) throws SQLException {
        PreparedStatement preparedStatement = this.sqlite.getStatement("INSERT INTO UserAnswers (userId, username, questionId, question, answer) VALUES (?, ?, ?, ?, ?)");
        preparedStatement.setString(1, userId);
        preparedStatement.setString(2, username);
        preparedStatement.setString(3, questionId);
        preparedStatement.setString(4, question);
        preparedStatement.setBoolean(5, answer);

        preparedStatement.execute();

        int pointModifier = this.cachedQuestions.get(questionId).getPointModifier();

        this.cachedAnswersByUser.get(userId).add(new CachedUserAnswer(questionId, answer, pointModifier));
        this.removeAnswerFromRemainingQuestionsForUserCache(userId, questionId);
    }

    private void buildQuestionCache() throws SQLException {
        PreparedStatement preparedStatement = this.sqlite.getStatement("SELECT questionId, question, questionNote, pointModifier FROM Questions");

        ResultSet resultSet = preparedStatement.executeQuery();

        while(resultSet.next()) {
            String questionId = resultSet.getString("questionId");
            String questionContent = resultSet.getString("question");
            String questionNote = resultSet.getString("questionNote");
            int pointModifier = resultSet.getInt("pointModifier");

            Question question = new Question(questionId, questionContent, questionNote, pointModifier);
            //System.out.println("Set modifier to " + question.getPointModifier() + " for question " + question.getQuestionId());
            this.cachedQuestions.put(questionId, question);
        }
    }

    public Question getQuestionById(String questionId) {
        return this.cachedQuestions.get(questionId);
    }

    public Question getNextQuestionForUser(String userId) throws SQLException {
        if(!this.cachedRemainingQuestionIdsByUser.containsKey(userId)) {
            this.buildRemainingQuestionsCacheForUser(userId);
        }

        Question question = this.getQuestionById(this.cachedRemainingQuestionIdsByUser.get(userId).get(0));

        return question;
    }

    public void removeAnswerFromRemainingQuestionsForUserCache(String userId, String questionId) throws SQLException {
        if(!this.cachedRemainingQuestionIdsByUser.containsKey(userId)) {
            this.buildRemainingQuestionsCacheForUser(userId);
        }

        List<String> cachedRemainingQuestionIdsForUser = this.cachedRemainingQuestionIdsByUser.get(userId);

        for(int index = 0; index < cachedRemainingQuestionIdsForUser.size(); index++) {
            if(cachedRemainingQuestionIdsForUser.get(index).equalsIgnoreCase(questionId)) {
                this.cachedRemainingQuestionIdsByUser.get(userId).remove(index);
            }
        }
    }

    public void buildRemainingQuestionsCacheForUser(String userId) throws SQLException {
        List<CachedUserAnswer> answers = this.getAlreadyAnsweredQuestionsForUser(userId);

        List<String> questionsForAnswers = new ArrayList<>();

        for(CachedUserAnswer answer : answers) {
            questionsForAnswers.add(answer.getQuestionId());
        }

        List<String> remainingQuestions = new ArrayList<>();
        remainingQuestions.addAll(this.allQuestionIds);

        for(String questionId : this.allQuestionIds) {
            boolean userAnsweredQuestion = questionsForAnswers.contains(questionId);

            if(userAnsweredQuestion) {
                remainingQuestions.remove(questionId);
            }
        }

        this.cachedRemainingQuestionIdsByUser.put(userId, remainingQuestions);
    }

    public void cacheAllQuestionIds() throws SQLException {
        ResultSet questionIdsResult = this.sqlite.getStatement("SELECT * FROM Questions").executeQuery();

        while(questionIdsResult.next()) {
            this.allQuestionIds.add(questionIdsResult.getString("questionId"));
        }

        System.out.println("Cached " + this.allQuestionIds.size() + " questions");
    }

    public List<CachedUserAnswer> getAlreadyAnsweredQuestionsForUser(String userId) throws SQLException {
        if(this.cachedAnswersByUser.containsKey(userId)) {
            return cachedAnswersByUser.get(userId);
        } else {
            List<CachedUserAnswer> cachedUserAnswers = new ArrayList<>();

            PreparedStatement fetchAnswersStatement = this.sqlite.getStatement("SELECT * FROM UserAnswers WHERE userId = ?");
            fetchAnswersStatement.setString(1, userId);

            ResultSet answersResult = fetchAnswersStatement.executeQuery();

            while(answersResult.next()) {
                String questionId = answersResult.getString("questionId");
                Question matchingQuestion = this.getQuestionById(questionId);
                int pointModifier = matchingQuestion.getPointModifier();
                cachedUserAnswers.add(new CachedUserAnswer(questionId, answersResult.getBoolean("answer"), pointModifier));
            }

            this.cachedAnswersByUser.put(userId, cachedUserAnswers);

            return cachedUserAnswers;
        }
    }

    public int calculateAndSetUserScore(String userId) throws SQLException {
        int score = 0;

        if(this.cachedAnswersByUser.get(userId) == null) {
            this.getAlreadyAnsweredQuestionsForUser(userId);
        }

        for(CachedUserAnswer cachedUserAnswer : this.cachedAnswersByUser.get(userId)) {
            if(cachedUserAnswer.questionAnswer == true) { //only modify if they say yes, otherwise the answer doesnt change their score
                score += cachedUserAnswer.getPointModifier();
                //System.out.println("Modifying score by " + cachedUserAnswer.getPointModifier() + " for answering yes to question " + cachedUserAnswer.getQuestionId());
            } else {
                //System.out.println("Answered no to question " + cachedUserAnswer.getQuestionId() + " so not modifying score");
            }
        }

        PreparedStatement preparedStatement = this.sqlite.getStatement("UPDATE UserMiscData SET currentScore = ? WHERE userId = ?");
        preparedStatement.setInt(1, score);
        preparedStatement.setString(2, userId);
        preparedStatement.execute();

        return score;
    }

    public int getUserScore(String userId) throws SQLException {
        return this.sqlite.getStatement("SELECT * FROM UserMiscData WHERE userId = '" + userId + "'").executeQuery().getInt("currentScore");
    }

    public void setUserScore(String userId, String username, int score) throws SQLException {
        boolean userHasScore = this.sqlite.getStatement("SELECT * FROM UserScores WHERE userId = '" + userId + "'").executeQuery().next();

        if(userHasScore) {
            PreparedStatement updateScoreStatement = this.sqlite.getStatement("UPDATE UserScores SET score = ? WHERE userId = ?");
            updateScoreStatement.setInt(1, score);
            updateScoreStatement.setString(2, userId);
        } else {
            PreparedStatement insertScoreStatement = this.sqlite.getStatement("INSERT INTO UserScores (userId, username, score) VALUES (?, ?, ?)");
            insertScoreStatement.setString(1, userId);
            insertScoreStatement.setString(2, username);
            insertScoreStatement.setInt(3, score);

            insertScoreStatement.execute();
        }
    }

    public int setupDefaultQuestions() throws SQLException, IOException {
        this.sqlite.getStatement("DELETE FROM Questions").execute();

        JSONArray questionsArray = new JSONArray(Util.getRequest("https://raw.githubusercontent.com/Consoomer-Consortium/SoyEvaluationBot/main/questions.json"));

        for(int index = 0; index < questionsArray.length(); index++) {
            JSONObject object = (JSONObject) questionsArray.get(index);

            int questionId = object.getInt("id");
            String question = object.getString("text");
            String questionNote = object.getString("note");
            int pointModifier = object.getInt("points");

            this.addQuestion(questionId, question, questionNote, pointModifier);
            //System.out.println("Added question: " + questionId + " / " + question + " / " + questionNote + " / " + pointModifier);
        }

        return questionsArray.length();
    }

    public void addQuestion(int questionId, String question, String questionNote, int pointModifier) throws SQLException {
        PreparedStatement preparedStatement = this.sqlite.getStatement("INSERT INTO Questions (questionId, question, questionNote, pointModifier) VALUES (?, ?, ?, ?)");
        preparedStatement.setInt(1, questionId);
        preparedStatement.setString(2, question);
        preparedStatement.setString(3, questionNote);
        preparedStatement.setInt(4, pointModifier);

        preparedStatement.execute();
    }

}

class CachedUserAnswer {

    public String questionId;
    public boolean questionAnswer;

    public int pointModifier;

    public CachedUserAnswer(String questionId, boolean questionAnswer, int pointModifier) {
        this.questionId = questionId;
        this.questionAnswer = questionAnswer;
        this.pointModifier = pointModifier;
    }

    public String getQuestionId() {
        return this.questionId;
    }

    public boolean getQuestionAnswer() {
        return this.questionAnswer;
    }

    public int getPointModifier() {
        return this.pointModifier;
    }

}

