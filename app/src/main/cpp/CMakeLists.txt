cmake_minimum_required(VERSION 3.22.1)
project("bypass_engine")

add_library(
        bypass_engine
        SHARED
        bypass_engine.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        bypass_engine
        ${log-lib})
