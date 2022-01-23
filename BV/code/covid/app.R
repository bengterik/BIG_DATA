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

library(treemap)

data(GNI2014, package = "treemap")

data <- read.csv("deaths_covid.csv")
all_columns <- colnames(data, do.NULL = TRUE, prefix = "col")
columns_wanted <- c("iso_code", "continent", "location", "date",
                   "total_cases_per_million", "total_deaths_per_million", 
                   "reproduction_rate", "total_boosters_per_hundred", 
                   "aged_65_older")

data_wanted <- data[columns_wanted]
# get the dates as a column to return the first and last day
dates <- as.Date(data_wanted$date)
first_day <- min(dates)
last_day <- max(dates)
chosen_day <- first_day

#continents <- dplyr::distinct(data_wanted, continent)[!apply(is.na(dplyr::distinct(data_wanted, continent)) | dplyr::distinct(data_wanted, continent) == "", 1, all),]



# Define UI for application that draws a histogram
ui <- shinyUI(fluidPage(
   
   # Application title
   titlePanel("Covid Data"),
   
   # Sidebar with a slider input for the choosen date
   
   sidebarLayout(
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
                              choices = c("total_cases_per_million",
                                             "total_deaths_per_million",
                                             "reproduction_rate",
                                             "total_boosters_per_hundred", 
                                             "aged_65_older")), selected = "total_deaths_per_million")),
        fluidRow(
          column(10,
                 selectInput("chosen_continent", h3("Choose the continent:"),
                             choices = list("Africa" = 1,
                                            "Asia" = 2,
                                            "Europe" = 3,
                                            "North America" = 4,
                                            "South America" = 5,
                                            "Oceania" = 6,
                                            "The whole world" = 7)), selected = 7))),
        mainPanel(
          highchartOutput("myMap") 
          )
      )
  )
)

url <- "https://code.highcharts.com/mapdata/custom/world-robinson-lowres.js"


# Define server logic required to draw a histogram
server <- function(input, output) {
  output$myMap <- renderHighchart({
    hcmap(
         "custom/africa", 
         data = data_wanted[data_wanted$date == input$date,],
         name = "Gross national income per capita", 
         value = input$chosen_variable,
         borderWidth = 0,
         nullColor = "#d3d3d3",
         joinBy = c("iso-a3", "iso_code")
     ) %>%
      hc_colorAxis(
        stops = color_stops(colors = viridisLite::inferno(10, begin = 0.1)),
        type = "logarithmic"
      )
})}

# Run the application 
shinyApp(ui = ui, server = server)

