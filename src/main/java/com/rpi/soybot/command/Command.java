package com.rpi.soybot.command;

import com.rpi.soybot.database.Database;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class Command {

    private final String NAME;
    private final String DESCRIPTION;

    private CommandListener commandListener;

    private List<OptionData> options = new ArrayList<>();

    public Command(String name, String description, CommandListener commandListener) {
        this.NAME = name;
        this.DESCRIPTION = description;
        this.commandListener = commandListener;
    }

    public abstract void onUse(JDA jda, SlashCommandInteractionEvent event) throws SQLException;

    public String getName() {
        return this.NAME;
    }

    public String getDescription() {
        return this.DESCRIPTION;
    }

    public Database getDatabase() {
        return this.commandListener.getDatabase();
    }

    public void addOption(OptionData optionData) {
        this.options.add(optionData);
    }

    public List<OptionData> getOptions() {
        return this.options;
    }

    public boolean hasOptions() {
        return this.options.size() > 0;
    }

}
