package com.flowstate;

import com.flowstate.cli.CliArguments;
import com.flowstate.cli.CliParser;
import com.flowstate.lifecycle.ApplicationLifecycle;

/**
 * Entry point for the flow-state CLI productivity timer.
 */
public final class FlowStateApplication {

    private FlowStateApplication() {
    }

    public static void main(String[] args) {
        CliParser parser = new CliParser();
        CliArguments cliArguments;

        try {
            cliArguments = parser.parse(args);
        } catch (IllegalArgumentException exception) {
            System.err.println("Error: " + exception.getMessage());
            System.err.println();
            System.err.println(parser.usage());
            System.exit(1);
            return;
        }

        if (cliArguments.showHelp()) {
            System.out.println(parser.usage());
            return;
        }

        int exitCode = new ApplicationLifecycle().run(cliArguments);
        System.exit(exitCode);
    }
}
