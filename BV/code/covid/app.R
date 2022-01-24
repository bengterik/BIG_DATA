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
                    "aged_65_older", "population","population_density")

data_wanted <- data[columns_wanted]
#data_wanted[is.na(data_wanted)] <- 0
data_wanted <- mutate_all(data_wanted, ~replace(., is.na(.), 0))
View(data_wanted)

# get the dates as a column to return the first and last day
dates <- as.Date(data_wanted$date)
first_day <- min(dates)
last_day <- max(dates)
chosen_day <- first_day

#continents <- dplyr::distinct(data_wanted, continent)[!apply(is.na(dplyr::distinct(data_wanted, continent)) | dplyr::distinct(data_wanted, continent) == "", 1, all),]



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
                  selectInput("chosen_variable", h3("Desired variable:"),
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
                             choices = list("Africa" = "custom/africa",
                                            "Asia" = "custom/asia",
                                            "Europe" = "custom/europe",
                                            "North America" = "custom/north-america",
                                            "South America" = "custom/south-america",
                                            "Oceania" = "custom/oceania",
                                            "The Whole World" = "custom/world-robinson-lowres")), 
                 selected = "The Whole World"))),
        mainPanel(
          highchartOutput("myMap") 
          )
      ),
   fluidRow(
     sidebarLayout(
       
       sidebarPanel(
         sliderInput(
           "bins", label = "Number of bins:",
           min = 1, value = 30, max = 50
         )
       ),
       
       mainPanel(
         plotOutput("distPlot")
       )
     )
  )
))

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
  
  output$distPlot <- renderPlot({
    # generate bins based on input$bins from ui.R
    x    <- faithful[, 2]
    bins <- seq(min(x), max(x), length.out = input$bins + 1)
    
    # draw the histogram with the specified number of bins
    hist(x, breaks = bins, col = 'darkgray', border = 'white')
  })
  
}

#color_stops(colors = viridisLite::inferno(10, begin = 0.1))
# Run the application 
shinyApp(ui = ui, server = server)

