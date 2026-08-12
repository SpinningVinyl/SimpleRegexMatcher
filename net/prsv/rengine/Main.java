package net.prsv.rengine;

import java.util.Scanner;

public class Main {

    private static String pattern;
    private static StateMachine machine;
    private static Scanner s = new Scanner(System.in);

    public static void setNewPattern() {
        String input = "";
        boolean isValid = false;
        while (!isValid) {
            System.out.println("Enter the regular expression: ");
            System.out.print("> ");
            if (!s.hasNextLine()) {
                System.out.println("Encountered EOF, quitting...");
                System.exit(0);
            }
            input = s.nextLine();
            try {
                machine = StateMachine.compile(input);
                pattern = input;
                isValid = true;
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

    }

    public static void main(String[] args) {

        boolean quit = false;
        setNewPattern();
        while(!quit) {
            System.out.println("Current regex: " + pattern + "\nEnter the input string, type ':regex' to set a new regex pattern, or ':quit' to exit: ");
            System.out.print("> ");
            if (!s.hasNextLine()) {
                System.out.println("Encountered EOF, quitting...");
                return;
            }
            String input = s.nextLine();
            if(input.trim().equalsIgnoreCase(":quit")) {
                quit = true;
                System.out.println("Bye!");
                continue;
            }
            if(input.trim().equalsIgnoreCase(":summary")) {
                System.out.println(machine.summary());
                continue;
            }
            if(input.trim().equalsIgnoreCase(":config")) {
                System.out.println(machine.config());
                continue;
            }
            if(input.trim().equalsIgnoreCase(":regex")) {
                setNewPattern();
                continue;
            }
            boolean accepted = machine.run(input);
            if (accepted) {
                System.out.println("String '" + input + "' accepted.");
            } else {
                System.out.println("String '" + input + "' rejected.");
            }
        }
        s.close();

    }
}
