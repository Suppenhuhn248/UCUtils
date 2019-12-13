package de.fuzzlemann.ucutils.utils.punishment;

import de.fuzzlemann.ucutils.base.command.ParameterParser;

/**
 * @author Fuzzlemann
 */
public class ViolationParser implements ParameterParser<String, Violation> {
    @Override
    public Violation parse(String input) {
        return PunishManager.getViolation(input.replace('-', ' '));
    }
}
