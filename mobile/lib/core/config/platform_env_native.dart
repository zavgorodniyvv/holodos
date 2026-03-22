import 'dart:io';

/// Native implementation using dart:io Platform.
String? getEnvVariable(String name) => Platform.environment[name];
