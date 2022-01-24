#
# This is a Shiny web application. You can run the application by clicking
# the 'Run App' button above.
#
# Find out more about building applications with Shiny here:
#
#    http://shiny.rstudio.com/
#


#install.packages("dplyr")

#source("covid/helpers.R")
# tryCatch(
#   {library(shiny)},
#   finally = install.packages("shiny")
# )
# tryCatch(
#   {library(maps)},
#   finally = install.packages("tmap")
# )
# tryCatch(
#   {library(mapproj)},
#   finally = install.packages("mapproj")
# )
# tryCatch(
#   {library(rworldmap)},
#   finally = install.packages("rworldmap")
# )
source("helpers.R")
library(highcharter)
library(ggplot2)
library(magrittr)
library(leaflet)
library(shiny)
library(jsonlite)
library(maps)
library(mapproj)
library(leaflet.providers)
library(dplyr)

data <- read.csv("deaths_covid.csv")
View(head(data,10))
all_columns <- colnames(data, do.NULL = TRUE, prefix = "col")
columns_wanted <- c("iso_code", "continent", "location", "date",
                    "total_cases_per_million","total_deaths_per_million",
                    "new_cases_smoothed_per_million", "new_deaths_smoothed_per_million", 
                    "weekly_icu_admissions_per_million", "weekly_hosp_admissions_per_million",
                    "reproduction_rate", "people_fully_vaccinated_per_hundred", "total_boosters_per_hundred", 
                    "aged_65_older")

data_wanted <- data[columns_wanted]
View(data_wanted)
countries <- distinct(data["location"])

# get the dates as a column to return the first and last day
dates <- as.Date(data_wanted$date)
first_day <- min(dates)
last_day <- max(dates)
chosen_day <- median(dates)

# A function to create a HighChart plot formatting input to strings
plot_high_chart <- function(.data,
                            chart_type = 'line',
                            x_value = "Year",             
                            y_value = "total",
                            group_value = "service") {
  
  if (is.character(x_value) ) {
    x_value <- ensym(x_value)
    
  }
  if (is.character(y_value)) {
    y_value <- ensym(y_value)
  }
  
  if (is.character(group_value)) {
    group_value <- ensym(group_value)
  }
  
  .data %>%
    hchart(chart_type, hcaes(x = !!x_value,
                             y = !!y_value,
                             group = !!group_value))
}

# Define UI for application that draws a histogram
ui <- shinyUI(
  fluidPage(
   
   # Application title
   titlePanel("Covid Data"),
   
   # Sidebar with a slider input for the chosen date
   
    fluidRow(
      sidebarPanel(
        fluidRow(
          column(10,sliderInput("date",
                     "Choose the date:",
                     min = first_day,
                     max = last_day,
                     value = chosen_day))),
         fluidRow(
           column(10,
                  selectInput("chosen_variable", h3("Chosen variable:"),
                              choices = list("Total Cases Per Million" = "total_cases_per_million" ,
                                             "Total Deaths Per Million" = "total_deaths_per_million" ,
                                             "New Cases Smoothed Per Million" = "new_cases_smoothed_per_million" ,
                                             "New Deaths Smoothed Per Million" = "new_deaths_smoothed_per_million" ,
                                             "Reproduction Rate" = "reproduction_rate" ,
                                             "Weekly ICU Admissions Per Million" = "weekly_icu_admissions_per_million" ,
                                             "Weekly Hosp Admissions Per Millionn" = "weekly_hosp_admissions_per_million" ,
                                             "People Fully Vaccinated Per Hunderd" = "people_fully_vaccinated_per_hundred" ,
                                             "Total Boosters Per Hundred" = "total_boosters_per_hundred" , 
                                             "Aged 65 And Older" = "aged_65_older")), selected = "New Cases Smoothed Per Million")),
        fluidRow(
          column(10,
                 selectInput("chosen_continent", h3("Choose the continent:"),
                             choices = list("The Whole World" = "custom/world-robinson-lowres",
                                            "Africa" = "custom/africa",
                                            "Asia" = "custom/asia",
                                            "Europe" = "custom/europe",
                                            "North America" = "custom/north-america",
                                            "South America" = "custom/south-america",
                                            "Oceania" = "custom/oceania"
                                            )), 
                 selected = "The Whole World"))),
        mainPanel(
          highchartOutput("myMap") 
          )
      ),
   fluidRow(
        titlePanel("Single comparison of two countries"),
        
        sidebarPanel(
          fluidRow(
            column(10,selectInput("chosenvar", h3("Chosen variable"),
                                  choices = list("Total Cases Per Million" = "total_cases_per_million" ,
                                                 "Total Deaths Per Million" = "total_deaths_per_million" ,
                                                 "New Cases Smoothed Per Million" = "new_cases_smoothed_per_million" ,
                                                 "New Deaths Smoothed Per Million" = "new_deaths_smoothed_per_million" ,
                                                 "Reproduction Rate" = "reproduction_rate" ,
                                                 "Weekly ICU Admissions Per Million" = "weekly_icu_admissions_per_million" ,
                                                 "Weekly Hosp Admissions Per Millionn" = "weekly_hosp_admissions_per_million" ,
                                                 "People Fully Vaccinated Per Hunderd" = "people_fully_vaccinated_per_hundred" ,
                                                 "Total Boosters Per Hundred" = "total_boosters_per_hundred")), selected = "New Cases Smoothed Per Million")),
          
          fluidRow(
            column(10,
                   selectInput("chosencountry_line1", h3("Choose the Country:"),
                               choices = countries), 
                   selected = "Germany")),
        
          fluidRow(
            column(10,
                   selectInput("chosencountry_line2", h3("Choose the Country:"),
                               choices = countries), 
                   selected = "Sweden"))
          ),
        
        mainPanel(
             highchartOutput("linePlot")
        )
   ),
   fluidRow(
     titlePanel("Compare two variables of one country"),
     
     sidebarPanel(
       fluidRow(
         column(10,selectInput("xaxis", h3("X-axis:"),
                               choices = list("Total Cases Per Million" = "total_cases_per_million" ,
                                              "Total Deaths Per Million" = "total_deaths_per_million" ,
                                              "New Cases Smoothed Per Million" = "new_cases_smoothed_per_million" ,
                                              "New Deaths Smoothed Per Million" = "new_deaths_smoothed_per_million" ,
                                              "Reproduction Rate" = "reproduction_rate" ,
                                              "Weekly ICU Admissions Per Million" = "weekly_icu_admissions_per_million" ,
                                              "Weekly Hosp Admissions Per Millionn" = "weekly_hosp_admissions_per_million" ,
                                              "People Fully Vaccinated Per Hunderd" = "people_fully_vaccinated_per_hundred" ,
                                              "Total Boosters Per Hundred" = "total_boosters_per_hundred")), selected = "New Cases Smoothed Per Million")),
       fluidRow(
         column(10,selectInput("yaxis", h3("Y-axis:"),
                               choices = list("Total Cases Per Million" = "total_cases_per_million" ,
                                              "Total Deaths Per Million" = "total_deaths_per_million" ,
                                              "New Cases Smoothed Per Million" = "new_cases_smoothed_per_million" ,
                                              "New Deaths Smoothed Per Million" = "new_deaths_smoothed_per_million" ,
                                              "Reproduction Rate" = "reproduction_rate" ,
                                              "Weekly ICU Admissions Per Million" = "weekly_icu_admissions_per_million" ,
                                              "Weekly Hosp Admissions Per Millionn" = "weekly_hosp_admissions_per_million" ,
                                              "People Fully Vaccinated Per Hunderd" = "people_fully_vaccinated_per_hundred" ,
                                              "Total Boosters Per Hundred" = "total_boosters_per_hundred")), selected = "Weekly ICU Admissions Per Million")),
       
       fluidRow(
         column(10,
                selectInput("chosencountry_scatter", h3("Choose the Country:"),
                            choices = countries), 
                selected = "Sweden"))
       ),
     
     mainPanel(
         highchartOutput("scatterPlot")
     )
   )
  )
)

# Maps https://code.highcharts.com/mapdata/
options(highcharter.download_map_data = TRUE)

# Define server logic required to draw a histogram
server <- function(input, output) {
    output$myMap <- renderHighchart({
      color_scale <- switch(input$chosen_variable,
                            "total_cases_per_million" =  c("#ffffff", "#520000"),
                            "total_deaths_per_million" = c("#ffffff", "#520000"),
                            "new_cases_smoothed_per_million" =  c("#ffffff", "#520000"),
                            "new_deaths_smoothed_per_million" =  c("#ffffff", "#520000"),
                            "weekly_icu_admissions_per_million" =  c("#ffffff", "#520000"),
                            "weekly_hosp_admissions_per_million" =  c("#ffffff", "#520000"),
                            "reproduction_rate" = c("#ffffff", "#61004f"),
                            "people_fully_vaccinated_per_hundred" = c("#ffffff", "#005208"), 
                            "total_boosters_per_hundred" = c("#ffffff", "#005208"), 
                            "aged_65_older" = c("#ffffff", "#003752"))
      label <- switch(input$chosen_variable,
                      "total_cases_per_million" =  "Total Cases Per Million",
                      "total_deaths_per_million" = "Total Deaths Per Million",
                      "new_cases_smoothed_per_million" = "New Cases Smoothed Per Million",
                      "new_deaths_smoothed_per_million" = "New Deaths Smoothed Per Million" ,
                      "reproduction_rate" = "Reproduction Rate",
                      "weekly_icu_admissions_per_million" = "Weekly ICU Admissions Per Million",
                      "weekly_hosp_admissions_per_million" = "Weekly Hosp Admissions Per Millionn",
                      "people_fully_vaccinated_per_hundred"= "People Fully Vaccinated Per Hundred"  ,
                      "total_boosters_per_hundred" = "Total Boosters Per Hundred", 
                      "aged_65_older" = "Aged 65 And Older")
      hcmap(
           input$chosen_continent, 
           data = data_wanted[data_wanted$date == input$date,],
           name = label, 
           value = input$chosen_variable,
           borderWidth = 0,
           nullColor = "#d3d3d3",
           joinBy = c("iso-a3", "iso_code")
       ) %>%
        hc_colorAxis(
          stops = color_stops(n = 10, colors = color_scale),
          type = "logarithmic"
        )
    })
    
    output$linePlot <- renderHighchart({
      df <- data_wanted[data_wanted$location == input$chosencountry_line1 | data_wanted$location == input$chosencountry_line2,]
      
      plot_high_chart(df, x_value="date", y_value = input$chosenvar, group = "location") %>%
        hc_colors(c("#d8b365","#5ab4ac"))

    })
    
    
    output$scatterPlot <- renderHighchart({
      df <- data_wanted[data_wanted$location == input$chosencountry_scatter,]
    
      plot_high_chart(df,chart_type = "scatter", x_value=input$xaxis, y_value = input$yaxis, group = "location") %>%
        hc_colors("#4789fc")
    })
}

#color_stops(colors = viridisLite::inferno(10, begin = 0.1))
# Run the application 
shinyApp(ui = ui, server = server)

