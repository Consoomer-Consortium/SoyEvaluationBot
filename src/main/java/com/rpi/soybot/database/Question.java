package com.rpi.soybot.database;

public class Question {

    private String questionId;

    private String question;

    private String questionNote;

    private int pointModifier;

    public Question(String questionId, String question, String questionNote, int pointModifier) {
        this.questionId = questionId;
        this.question = question;
        this.questionNote = questionNote;
        this.pointModifier = pointModifier;
    }

    public String getQuestionId() {
        return this.questionId;
    }

    public String getQuestion() {
        return this.question;
    }

    public String getQuestionNote() {
        return this.questionNote;
    }

    public int getPointModifier() {
        return this.pointModifier;
    }

    public boolean hasNote() {
        return this.questionNote.length() > 0;
    }

}
