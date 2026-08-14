package com.example.second;

import javax.swing.*;
import java.io.IOException;

public class TestClass {
    public static void main(String[] args) throws IOException {
        SwingUtilities.invokeLater(FirstForm::new);
    }
}
