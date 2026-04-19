package com.example.second;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class TestClass {
    public static void main(String[] args) throws IOException {
        SwingUtilities.invokeLater(FirstForm::new);
    }
}
