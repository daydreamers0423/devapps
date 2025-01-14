cmake_minimum_required(VERSION 3.4.1)

# Define the library
add_library(
        native-lib  # Name of the shared library
        SHARED
        ./native-lib.cpp  # Source file(s)
)

# Specify the libraries to link with
find_library(
        log-lib
        log
)

target_link_libraries(
        native-lib
        ${log-lib}
)
